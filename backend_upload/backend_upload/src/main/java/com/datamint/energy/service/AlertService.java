package com.datamint.energy.service;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 6 - Smart alerts incl. usage-anomaly detection. Keeps the worst event per type per day. */
@Service
public class AlertService {
    private final AlertRepository alerts; private final MeterReadingRepository readings;
    private final AnalyticsService analytics; private final SettingService settings;

    public AlertService(AlertRepository alerts, MeterReadingRepository readings, AnalyticsService analytics, SettingService settings) {
        this.alerts = alerts; this.readings = readings; this.analytics = analytics; this.settings = settings;
    }

    private static String slot(MeterReading r) { return r.readingTime.getHour() + "|" + r.weekStatus; }

    private Alert mk(String type, String sev, String title, String msg, String meter, LocalDateTime t, double v) {
        Alert a = new Alert(); a.type = type; a.severity = sev; a.title = title; a.message = msg;
        a.meterCode = meter; a.triggeredAt = t; a.readingValue = v; return a;
    }

    private void cand(Map<String, Alert> best, Map<String, Double> score, String key, double s, Alert a) {
        if (!score.containsKey(key) || s > score.get(key)) { score.put(key, s); best.put(key, a); }
    }

    @Transactional
    public int scan(String meter, int days) {
        LocalDateTime end = analytics.now(meter), from = end.minusDays(days);
        List<MeterReading> all = readings.range(meter, LocalDateTime.of(2000, 1, 1, 0, 0), end);
        double hi = settings.num("alert.highKwh", 100), lowPf = settings.num("alert.lowPf", 70), z = settings.num("alert.anomalyZ", 3);

        Map<String, double[]> st = new HashMap<>();   // baseline per hour-of-day + week status: sum, sumSq, count
        for (MeterReading r : all) {
            double[] a = st.computeIfAbsent(slot(r), k -> new double[3]);
            a[0] += r.usageKwh; a[1] += r.usageKwh * r.usageKwh; a[2]++;
        }
        Map<String, Alert> best = new HashMap<>(); Map<String, Double> score = new HashMap<>();
        MeterReading prev = null;
        for (MeterReading r : all) {
            if (r.readingTime.isAfter(from)) {
                String day = r.readingTime.toLocalDate().toString();
                if (r.usageKwh >= hi)
                    cand(best, score, "H|" + day, r.usageKwh, mk("HIGH_CONSUMPTION", r.usageKwh >= hi * 1.2 ? "CRITICAL" : "HIGH",
                            "High energy consumption", String.format("%.0f kW (%.1f kWh in 15 min)", r.usageKwh * 4, r.usageKwh), meter, r.readingTime, r.usageKwh));
                if (r.lagPf < lowPf && r.usageKwh > 15)
                    cand(best, score, "P|" + day, 100 - r.lagPf, mk("LOW_POWER_FACTOR", "MEDIUM",
                            "Power factor low", String.format("%.2f", r.lagPf / 100), meter, r.readingTime, r.lagPf));
                double[] a = st.get(slot(r));
                if (a != null && a[2] > 10) {
                    double mean = a[0] / a[2], sd = Math.sqrt(Math.max(0, a[1] / a[2] - mean * mean));
                    double zs = sd > 0 ? (r.usageKwh - mean) / sd : 0;
                    if (r.usageKwh > 10 && zs >= z)
                        cand(best, score, "A|" + day, zs, mk("USAGE_ANOMALY", zs > z + 1 ? "HIGH" : "MEDIUM", "Usage anomaly detected",
                                String.format("%.1f kWh vs usual %.1f kWh for this hour", r.usageKwh, mean), meter, r.readingTime, r.usageKwh));
                }
                if (prev != null && Duration.between(prev.readingTime, r.readingTime).toMinutes() > 30)
                    cand(best, score, "G|" + day, Duration.between(prev.readingTime, r.readingTime).toMinutes(), mk("METER_COMM_FAILURE", "MEDIUM",
                            "Meter communication gap", "No data for " + Duration.between(prev.readingTime, r.readingTime).toMinutes() + " min", meter, r.readingTime, 0));
            }
            prev = r;
        }
        int saved = 0;
        for (Alert a : best.values())
            if (!alerts.existsByTypeAndMeterCodeAndTriggeredAt(a.type, a.meterCode, a.triggeredAt)) { alerts.save(a); saved++; }
        return saved;
    }

    /** Called when a new reading arrives through POST /api/readings. */
    public void check(MeterReading r) {
        double hi = settings.num("alert.highKwh", 100);
        if (r.usageKwh >= hi && !alerts.existsByTypeAndMeterCodeAndTriggeredAt("HIGH_CONSUMPTION", r.meterCode, r.readingTime))
            alerts.save(mk("HIGH_CONSUMPTION", "HIGH", "High energy consumption", String.format("%.0f kW", r.usageKwh * 4), r.meterCode, r.readingTime, r.usageKwh));
    }
}
