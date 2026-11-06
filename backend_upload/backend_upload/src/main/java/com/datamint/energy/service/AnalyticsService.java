package com.datamint.energy.service;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Modules 2, 4, 8 and efficiency reports: consumption, peak/off-peak, department-wise, carbon, efficiency. */
@Service
public class AnalyticsService {
    public static final String DEFAULT_METER = "MTR-001";
    private final MeterReadingRepository readings;
    private final DepartmentRepository departments;
    private final TariffRepository tariffs;
    private final SettingService settings;

    public AnalyticsService(MeterReadingRepository readings, DepartmentRepository departments, TariffRepository tariffs, SettingService settings) {
        this.readings = readings; this.departments = departments; this.tariffs = tariffs; this.settings = settings;
    }

    // ---------- helpers ----------
    /**
     * "Now" for all dashboards and reports.
     * Live replay (default): the dataset's last day is replayed in step with the wall clock, so the
     * data advances by one 15-minute reading every 15 real minutes (00:00 -> 23:45, then it starts again).
     * Set the setting "live.replay" to 0 to freeze "now" at the newest reading instead.
     */
    public LocalDateTime now(String meter) {
        LocalDateTime end = readings.latest(meter);
        if (end == null) return LocalDateTime.now();
        if (settings.num("live.replay", 1) == 0) return end;
        LocalTime t = LocalTime.now();
        LocalDateTime live = end.toLocalDate().atTime(t.getHour(), t.getMinute() / 15 * 15);
        return live.isAfter(end) ? end : live;
    }
    public List<MeterReading> window(String meter, int days, int offsetDays) {
        LocalDateTime end = now(meter).minusDays(offsetDays);
        return readings.range(meter, end.minusDays(days), end);
    }
    public Tariff tariff() {
        return tariffs.findFirstByActiveTrue().orElseGet(Tariff::new);
    }
    public static double sum(List<MeterReading> l) { return l.stream().mapToDouble(r -> r.usageKwh).sum(); }
    public static double r1(double v) { return Math.round(v * 10) / 10.0; }
    public static double r2(double v) { return Math.round(v * 100) / 100.0; }
    public static double pct(double a, double b) { return b == 0 ? 0 : r1((a - b) / b * 100); }
    public static boolean isPeak(Tariff t, LocalDateTime time) {
        int h = time.getHour();
        return h >= t.peakStartHour && h < t.peakEndHour;
    }
    public TreeMap<LocalDate, Double> daily(List<MeterReading> l) {
        TreeMap<LocalDate, Double> m = new TreeMap<>();
        l.forEach(r -> m.merge(r.readingTime.toLocalDate(), r.usageKwh, Double::sum));
        return m;
    }

    // ---------- Module 4: consumption analysis (daily / weekly / monthly) ----------
    public List<Map<String, Object>> consumption(String meter, String period, int days) {
        Function<MeterReading, String> key;
        switch (period) {
            case "monthly": key = r -> YearMonth.from(r.readingTime).toString(); break;
            case "weekly": key = r -> r.readingTime.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString(); break;
            default: key = r -> r.readingTime.toLocalDate().toString();
        }
        Map<String, List<MeterReading>> g = window(meter, days, 0).stream()
                .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        Double prev = null;
        for (Map.Entry<String, List<MeterReading>> e : g.entrySet()) {
            List<MeterReading> l = e.getValue();
            double kwh = sum(l);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("period", e.getKey());
            row.put("kwh", r1(kwh));
            row.put("avgKw", r1(kwh / l.size() * 4));
            row.put("peakKw", r1(l.stream().mapToDouble(r -> r.usageKwh).max().orElse(0) * 4));
            row.put("avgPowerFactor", r1(l.stream().mapToDouble(r -> r.lagPf).average().orElse(0)));
            row.put("co2Tco2", r2(l.stream().mapToDouble(r -> r.co2Tco2).sum()));
            row.put("changePercent", prev == null ? null : pct(kwh, prev));
            prev = kwh;
            out.add(row);
        }
        return out;
    }

    public Map<String, Object> peakOffPeak(String meter, int days) {
        Tariff t = tariff();
        List<MeterReading> w = window(meter, days, 0);
        double peak = w.stream().filter(r -> isPeak(t, r.readingTime)).mapToDouble(r -> r.usageKwh).sum();
        double total = sum(w);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("peakWindow", t.peakStartHour + ":00 - " + t.peakEndHour + ":00");
        m.put("peakKwh", r1(peak)); m.put("offPeakKwh", r1(total - peak));
        m.put("peakSharePercent", total == 0 ? 0 : r1(peak / total * 100));
        return m;
    }

    public List<Map<String, Object>> byLoadType(String meter, int days) {
        List<MeterReading> w = window(meter, days, 0);
        double total = sum(w);
        Map<String, List<MeterReading>> g = w.stream().collect(Collectors.groupingBy(r -> r.loadType, TreeMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        g.forEach((k, l) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("loadType", k); row.put("kwh", r1(sum(l)));
            row.put("sharePercent", total == 0 ? 0 : r1(sum(l) / total * 100));
            row.put("hours", r1(l.size() * 0.25));
            out.add(row);
        });
        return out;
    }

    // ---------- Module 4 (department-wise consumption) ----------
    public List<Map<String, Object>> departmentWise(String meter, int days) {
        List<MeterReading> w = window(meter, days, 0);
        double total = sum(w), co2 = w.stream().mapToDouble(r -> r.co2Tco2).sum();
        double rate = tariff().energyRate;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Department d : departments.findByStatus("ACTIVE")) {
            if (!meter.equals(d.meterCode)) continue;
            double kwh = total * d.allocationPercent / 100.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("department", d.name);
            row.put("building", d.building == null ? null : d.building.name);
            row.put("sharePercent", d.allocationPercent);
            row.put("kwh", r1(kwh));
            row.put("estimatedCostInr", Math.round(kwh * rate));
            row.put("co2Tco2", r2(co2 * d.allocationPercent / 100.0));
            row.put("targetKwh", d.monthlyTargetKwh == null ? null : d.monthlyTargetKwh * days / 30.0);
            row.put("status", d.monthlyTargetKwh == null ? "NO_TARGET" : (kwh > d.monthlyTargetKwh * days / 30.0 ? "OVER_TARGET" : "WITHIN_TARGET"));
            out.add(row);
        }
        out.sort((a, b) -> Double.compare((double) b.get("kwh"), (double) a.get("kwh")));
        for (int i = 0; i < out.size(); i++) out.get(i).put("rank", i + 1);
        return out;
    }

    // ---------- Module 8: carbon footprint ----------
    public Map<String, Object> carbon(String meter, int days) {
        List<MeterReading> w = window(meter, days, 0), p = window(meter, days, days);
        double cur = w.stream().mapToDouble(r -> r.co2Tco2).sum(), prev = p.stream().mapToDouble(r -> r.co2Tco2).sum();
        TreeMap<String, Double> dayMap = new TreeMap<>();
        w.forEach(r -> dayMap.merge(r.readingTime.toLocalDate().toString(), r.co2Tco2, Double::sum));
        double target = settings.num("carbon.targetTco2PerMonth", 0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("periodDays", days); m.put("totalTco2", r2(cur)); m.put("previousPeriodTco2", r2(prev));
        m.put("changePercent", pct(cur, prev));
        m.put("kgCo2PerKwh", sum(w) == 0 ? 0 : r2(cur * 1000 / sum(w)));
        m.put("targetTco2PerMonth", target);
        List<Map<String, Object>> d = new ArrayList<>();
        dayMap.forEach((k, v) -> { Map<String, Object> x = new LinkedHashMap<>(); x.put("date", k); x.put("tco2", r2(v)); d.add(x); });
        m.put("daily", d);
        List<Map<String, Object>> dept = new ArrayList<>();
        for (Map<String, Object> x : departmentWise(meter, days)) {
            Map<String, Object> y = new LinkedHashMap<>(); y.put("department", x.get("department")); y.put("tco2", x.get("co2Tco2")); dept.add(y);
        }
        m.put("departments", dept);
        return m;
    }

    // ---------- Efficiency report (monthly) ----------
    public List<Map<String, Object>> efficiency(String meter) {
        double targetMonthly = settings.num("target.monthlyKwh", 80000);
        List<MeterReading> all = readings.range(meter, LocalDateTime.of(2000, 1, 1, 0, 0), now(meter));
        Map<String, List<MeterReading>> g = all.stream().collect(Collectors.groupingBy(r -> YearMonth.from(r.readingTime).toString(), TreeMap::new, Collectors.toList()));
        List<Map<String, Object>> out = new ArrayList<>();
        g.forEach((month, l) -> {
            double kwh = sum(l);
            double opHours = l.stream().filter(r -> !"Light_Load".equals(r.loadType)).count() * 0.25;
            double avgKw = kwh / l.size() * 4, peakKw = l.stream().mapToDouble(r -> r.usageKwh).max().orElse(0) * 4;
            double days = l.size() / 96.0, target = targetMonthly * days / 30.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month); row.put("kwh", r1(kwh)); row.put("operatingHours", r1(opHours));
            row.put("kwhPerOperatingHour", opHours == 0 ? 0 : r1(kwh / opHours));
            row.put("loadFactorPercent", peakKw == 0 ? 0 : r1(avgKw / peakKw * 100));
            row.put("targetKwh", Math.round(target)); row.put("variancePercent", pct(kwh, target));
            row.put("partialMonth", l.size() < YearMonth.parse(month).lengthOfMonth() * 96);
            out.add(row);
        });
        return out;
    }
}
