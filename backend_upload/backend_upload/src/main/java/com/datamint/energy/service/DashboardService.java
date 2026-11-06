package com.datamint.energy.service;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import static com.datamint.energy.service.AnalyticsService.*;

/** Module 1 - Dashboard. Returns exactly the JSON shape the dashboard page (index.html) renders. */
@Service
public class DashboardService {
    private final AnalyticsService an; private final MeterReadingRepository readings; private final AlertRepository alerts; private final OptimizationService opt;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH);

    public DashboardService(AnalyticsService an, MeterReadingRepository readings, AlertRepository alerts, OptimizationService opt) {
        this.an = an; this.readings = readings; this.alerts = alerts; this.opt = opt;
    }

    private Map<String, Object> pack(List<String> l, List<Double> v) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("l", l); m.put("v", v.stream().map(AnalyticsService::r1).collect(Collectors.toList())); return m;
    }

    public Map<String, Object> dashboard(String meter) {
        LocalDateTime end = an.now(meter);
        Tariff tariff = an.tariff();

        // ---- chart ranges: 1D (2-hour), 1W, 1M (daily), 3M (weekly), 1Y (monthly) ----
        Map<String, Object> ch = new LinkedHashMap<>();
        TreeMap<Integer, Double> h = new TreeMap<>();
        an.window(meter, 1, 0).forEach(r -> h.merge(r.readingTime.getHour() / 2 * 2, r.usageKwh, Double::sum));
        ch.put("1D", pack(h.keySet().stream().map(x -> String.format("%02d:00", x)).collect(Collectors.toList()), new ArrayList<>(h.values())));
        TreeMap<LocalDate, Double> d7 = an.daily(an.window(meter, 7, 0)), d30 = an.daily(an.window(meter, 30, 0));
        ch.put("1W", pack(d7.keySet().stream().map(d -> d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).collect(Collectors.toList()), new ArrayList<>(d7.values())));
        ch.put("1M", pack(d30.keySet().stream().map(d -> d.format(DAY)).collect(Collectors.toList()), new ArrayList<>(d30.values())));

        // 3M: weekly totals (full weeks only)
        Map<LocalDate, List<MeterReading>> weeks = an.window(meter, 92, 0).stream().collect(Collectors.groupingBy(
                r -> r.readingTime.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), TreeMap::new, Collectors.toList()));
        List<String> wl = new ArrayList<>(); List<Double> wv = new ArrayList<>();
        weeks.forEach((d, l) -> { if (l.size() >= 672) { wl.add(d.format(DAY)); wv.add(sum(l)); } });
        ch.put("3M", pack(wl, wv));

        // 1Y: monthly totals (full months only)
        Map<YearMonth, List<MeterReading>> months = an.window(meter, 365, 0).stream().collect(Collectors.groupingBy(
                r -> YearMonth.from(r.readingTime), TreeMap::new, Collectors.toList()));
        List<String> ml = new ArrayList<>(); List<Double> mv = new ArrayList<>();
        months.forEach((ym, l) -> { if (l.size() >= 96 * 27) { ml.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)); mv.add(sum(l)); } });
        ch.put("1Y", pack(ml, mv));

        // ---- KPI cards: last 30 days vs previous 30 days ----
        List<MeterReading> cur = an.window(meter, 30, 0), prv = an.window(meter, 30, 30);
        double kwh = sum(cur), pk = sum(prv);
        double pf = cur.stream().mapToDouble(r -> r.lagPf).average().orElse(0), ppf = prv.stream().mapToDouble(r -> r.lagPf).average().orElse(0);
        double co2 = cur.stream().mapToDouble(r -> r.co2Tco2).sum(), pco2 = prv.stream().mapToDouble(r -> r.co2Tco2).sum();
        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("kwh", Math.round(kwh)); cards.put("kwhp", pct(kwh, pk));
        cards.put("cost", Math.round(kwh * tariff.energyRate)); cards.put("costp", pct(kwh, pk));
        cards.put("pf", r1(pf)); cards.put("pfp", pct(pf, ppf));
        cards.put("co2", r1(co2)); cards.put("co2p", pct(co2, pco2));
        cards.put("spark", d30.values().stream().skip(Math.max(0, d30.size() - 14)).map(v -> (double) Math.round(v)).collect(Collectors.toList()));

        // ---- potential savings (sum of rule-based recommendations) ----
        double potential = opt.recommendations(meter).stream().mapToDouble(rec -> ((Number) rec.get("estimatedMonthlySavingsInr")).doubleValue()).sum();
        cards.put("savings", kwh == 0 ? 0 : r1(potential / (kwh * tariff.energyRate) * 100));
        cards.put("savingsInr", Math.round(potential));

        // ---- distribution by load type ----
        Map<String, Double> dist = new LinkedHashMap<>();
        cur.stream().collect(Collectors.groupingBy(r -> r.loadType, Collectors.summingDouble(r -> r.usageKwh)))
                .forEach((k, v) -> dist.put(k, r1(v / kwh * 100)));

        // ---- real-time monitoring: latest 4 readings ----
        List<Map<String, Object>> mon = new ArrayList<>();
        for (MeterReading r : readings.findByMeterCodeOrderByReadingTimeDesc(meter, PageRequest.of(0, 4)).getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("t", r.readingTime.toLocalTime().toString().substring(0, 5)); m.put("kwh", r.usageKwh);
            m.put("kw", r1(r.usageKwh * 4)); m.put("pf", r.lagPf); m.put("lt", r.loadType);
            mon.add(m);
        }

        // ---- alerts: latest open alerts + peak demand info ----
        List<Map<String, Object>> al = new ArrayList<>();
        for (Alert a : latestPerType()) {
            String k = ("CRITICAL".equals(a.severity) || "HIGH".equals(a.severity)) ? "r" : "MEDIUM".equals(a.severity) ? "y" : "b";
            al.add(alertRow(k, a.title, a.meterCode + " · " + a.message, a.triggeredAt.format(STAMP)));
        }
        cur.stream().max(Comparator.comparingDouble(r -> r.usageKwh)).ifPresent(r ->
                al.add(alertRow("b", "Peak demand this month", meter + " · " + Math.round(r.usageKwh * 4) + " kW", r.readingTime.format(STAMP))));

        // ---- departments (simulated allocation of the single meter): donut + monitoring rows ----
        double todayKwh = sum(an.window(meter, 1, 0));
        List<Map<String, Object>> dep = new ArrayList<>();
        for (Map<String, Object> dd : an.departmentWise(meter, 30)) {
            double share = ((Number) dd.get("sharePercent")).doubleValue();
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("name", dd.get("department")); x.put("pct", share); x.put("kwhToday", r1(todayKwh * share / 100));
            Object tg = dd.get("targetKwh");
            x.put("usedPercent", tg == null ? null : r1(((Number) dd.get("kwh")).doubleValue() / ((Number) tg).doubleValue() * 100));
            x.put("status", "OVER_TARGET".equals(dd.get("status")) ? "High Load" : "Normal");
            dep.add(x);
        }

        double maxShare = dist.getOrDefault("Maximum_Load", 0.0);
        String tip = "Maximum-load intervals use " + Math.round(maxShare) + "% of your energy. Shifting part of that load to off-peak hours can cut peak demand and cost.";

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("end", end.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)));
        d.put("ch", ch); d.put("cards", cards); d.put("dist", dist); d.put("mon", mon); d.put("al", al);
        d.put("dep", dep); d.put("openAlerts", alerts.countByStatus("OPEN"));
        d.put("tip", tip); d.put("tar", tariff.energyRate);
        return d;
    }

    private List<Alert> latestPerType() {
        List<Alert> l = new ArrayList<>();
        for (String t : new String[]{"HIGH_CONSUMPTION", "LOW_POWER_FACTOR", "USAGE_ANOMALY"})
            alerts.findFirstByTypeAndStatusOrderByTriggeredAtDesc(t, "OPEN").ifPresent(l::add);
        return l;
    }

    private Map<String, Object> alertRow(String k, String a, String b, String t) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("k", k); m.put("a", a); m.put("b", b); m.put("t", t); return m;
    }
}
