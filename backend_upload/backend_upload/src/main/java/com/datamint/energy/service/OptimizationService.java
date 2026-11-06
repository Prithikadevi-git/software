package com.datamint.energy.service;

import com.datamint.energy.model.*;
import java.util.*;
import org.springframework.stereotype.Service;

import static com.datamint.energy.service.AnalyticsService.*;

/** Module 7 - Rule-based energy optimization recommendations (30-day window). */
@Service
public class OptimizationService {
    private final AnalyticsService an;
    public OptimizationService(AnalyticsService an) { this.an = an; }

    private Map<String, Object> rec(String title, String category, String detail, double savings) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title); m.put("category", category); m.put("detail", detail);
        m.put("estimatedMonthlySavingsInr", Math.round(savings)); return m;
    }

    public List<Map<String, Object>> recommendations(String meter) {
        Tariff t = an.tariff();
        List<MeterReading> w = an.window(meter, 30, 0);
        List<Map<String, Object>> out = new ArrayList<>();
        if (w.isEmpty()) return out;
        double total = sum(w), cost = total * t.energyRate;
        double peakKwh = w.stream().filter(r -> isPeak(t, r.readingTime)).mapToDouble(r -> r.usageKwh).sum();
        double peakKw = w.stream().mapToDouble(r -> r.usageKwh).max().orElse(0) * 4, avgKw = total / w.size() * 4;
        double pf = w.stream().mapToDouble(r -> r.lagPf).average().orElse(100);
        double lightKwh = w.stream().filter(r -> "Light_Load".equals(r.loadType)).mapToDouble(r -> r.usageKwh).sum();

        out.add(rec("Shift flexible load out of peak hours", "LOAD_SHIFTING",
                String.format("%.0f%% of energy is used between %d:00 and %d:00. Moving 10%% of it to off-peak hours reduces cost under time-of-day tariffs.",
                        peakKwh / total * 100, t.peakStartHour, t.peakEndHour), 0.10 * peakKwh * Math.max(0, t.peakRate - t.energyRate)));
        out.add(rec("Reduce peak demand", "PEAK_DEMAND",
                String.format("Peak demand is %.0f kW against an average of %.0f kW. Stagger motor and furnace start-ups to cut the peak by 10%%.", peakKw, avgKw),
                0.10 * peakKw * t.demandRatePerKw));
        if (pf < 90)
            out.add(rec("Improve power factor", "POWER_FACTOR",
                    String.format("Average lagging power factor is %.1f%%. A capacitor bank can lift it above 95%% and avoid low-PF penalties.", pf), 0.02 * cost));
        out.add(rec("Switch off idle equipment in light-load hours", "BASE_LOAD",
                String.format("Light-load intervals consumed %.0f kWh in 30 days. Scheduling shutdowns can save about 10%% of this.", lightKwh), 0.10 * lightKwh * t.energyRate));
        out.sort((a, b) -> Long.compare((long) b.get("estimatedMonthlySavingsInr"), (long) a.get("estimatedMonthlySavingsInr")));
        for (int i = 0; i < out.size(); i++) out.get(i).put("priority", i == 0 ? "HIGH" : i == 1 ? "MEDIUM" : "LOW");
        return out;
    }
}
