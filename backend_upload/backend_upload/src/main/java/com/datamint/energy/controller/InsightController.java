package com.datamint.energy.controller;

import com.datamint.energy.model.Alert;
import com.datamint.energy.repository.AlertRepository;
import com.datamint.energy.service.*;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Read-only analytics: modules 1, 4, 5, 7, 8, 10 (+ efficiency and monthly cost comparison). */
@RestController @RequestMapping("/api")
public class InsightController {
    private static final String M = AnalyticsService.DEFAULT_METER;
    private final DashboardService dashboard; private final AnalyticsService an; private final CostService cost;
    private final OptimizationService opt; private final AlertRepository alerts;

    public InsightController(DashboardService dashboard, AnalyticsService an, CostService cost, OptimizationService opt, AlertRepository alerts) {
        this.dashboard = dashboard; this.an = an; this.cost = cost; this.opt = opt; this.alerts = alerts;
    }

    // Module 1
    @GetMapping("/dashboard") public Map<String, Object> dashboard(@RequestParam(defaultValue = M) String meter) { return dashboard.dashboard(meter); }

    // Module 4
    @GetMapping("/analytics/consumption")
    public List<Map<String, Object>> consumption(@RequestParam(defaultValue = M) String meter, @RequestParam(defaultValue = "daily") String period, @RequestParam(defaultValue = "30") int days) {
        return an.consumption(meter, period, days);
    }
    @GetMapping("/analytics/peak-offpeak") public Map<String, Object> peak(@RequestParam(defaultValue = M) String meter, @RequestParam(defaultValue = "30") int days) { return an.peakOffPeak(meter, days); }
    @GetMapping("/analytics/load-type") public List<Map<String, Object>> loadType(@RequestParam(defaultValue = M) String meter, @RequestParam(defaultValue = "30") int days) { return an.byLoadType(meter, days); }
    @GetMapping("/analytics/department-wise") public List<Map<String, Object>> dept(@RequestParam(defaultValue = M) String meter, @RequestParam(defaultValue = "30") int days) { return an.departmentWise(meter, days); }
    @GetMapping("/analytics/efficiency") public List<Map<String, Object>> efficiency(@RequestParam(defaultValue = M) String meter) { return an.efficiency(meter); }

    // Module 5
    @GetMapping("/cost/estimate")
    public Map<String, Object> estimate(@RequestParam(defaultValue = M) String meter, @RequestParam(required = false) String month) {
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.from(an.now(meter)).minusMonths(1);
        return cost.estimate(meter, ym);
    }
    @GetMapping("/cost/monthly-comparison") public List<Map<String, Object>> comparison(@RequestParam(defaultValue = M) String meter) { return cost.monthlyComparison(meter); }

    // Module 7
    @GetMapping("/optimization/recommendations") public List<Map<String, Object>> recommendations(@RequestParam(defaultValue = M) String meter) { return opt.recommendations(meter); }

    // Module 8
    @GetMapping("/carbon/summary") public Map<String, Object> carbon(@RequestParam(defaultValue = M) String meter, @RequestParam(defaultValue = "30") int days) { return an.carbon(meter, days); }

    // Module 10 - reports as JSON, or CSV with ?format=csv
    @GetMapping("/reports/{type}")
    public ResponseEntity<?> report(@PathVariable String type, @RequestParam(defaultValue = M) String meter, @RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "json") String format) {
        List<Map<String, Object>> rows;
        switch (type) {
            case "daily": case "weekly": case "monthly": rows = an.consumption(meter, type, type.equals("monthly") ? 183 : days); break;
            case "cost": rows = cost.monthlyComparison(meter); break;
            case "efficiency": rows = an.efficiency(meter); break;
            case "department": rows = an.departmentWise(meter, days); break;
            case "alerts":
                rows = alerts.findAllByOrderByTriggeredAtDesc().stream().map(this::alertRow).collect(Collectors.toList()); break;
            default: return ResponseEntity.badRequest().body(Map.of("error", "Unknown report. Use daily, weekly, monthly, cost, efficiency, department, alerts"));
        }
        if (!"csv".equals(format)) return ResponseEntity.ok(rows);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + type + ".csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(toCsv(rows));
    }

    private Map<String, Object> alertRow(Alert a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", a.triggeredAt); m.put("type", a.type); m.put("severity", a.severity); m.put("message", a.message); m.put("status", a.status); m.put("assignedTo", a.assignedTo);
        return m;
    }

    private String toCsv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return "";
        List<String> cols = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder(String.join(",", cols)).append("\n");
        for (Map<String, Object> r : rows)
            sb.append(cols.stream().map(c -> { Object v = r.get(c); String s = v == null ? "" : v.toString(); return s.contains(",") ? "\"" + s + "\"" : s; }).collect(Collectors.joining(","))).append("\n");
        return sb.toString();
    }
}
