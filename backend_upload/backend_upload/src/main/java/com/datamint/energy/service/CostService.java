package com.datamint.energy.service;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static com.datamint.energy.service.AnalyticsService.*;

/** Module 5 - Electricity cost: estimate from tariff, compare with actual bill, month-to-month comparison. */
@Service
public class CostService {
    private final AnalyticsService an; private final MeterReadingRepository readings;
    private final EnergyBillRepository bills; private final DepartmentRepository departments;

    public CostService(AnalyticsService an, MeterReadingRepository readings, EnergyBillRepository bills, DepartmentRepository departments) {
        this.an = an; this.readings = readings; this.bills = bills; this.departments = departments;
    }

    public Map<String, Object> estimate(String meter, YearMonth ym) {
        Tariff t = an.tariff();
        LocalDateTime from = ym.atDay(1).atStartOfDay().minusSeconds(1), to = ym.plusMonths(1).atDay(1).atStartOfDay().minusSeconds(1);
        List<MeterReading> rs = readings.range(meter, from, to);
        double kwh = 0, energy = 0, peakKwh = 0, maxKw = 0;
        for (MeterReading r : rs) {
            kwh += r.usageKwh; maxKw = Math.max(maxKw, r.usageKwh * 4);
            if (isPeak(t, r.readingTime)) { peakKwh += r.usageKwh; energy += r.usageKwh * t.peakRate; } else energy += r.usageKwh * t.energyRate;
        }
        double demand = maxKw * t.demandRatePerKw, sub = energy + demand + t.fixedCharge, tax = sub * t.taxPercent / 100, total = sub + tax;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("month", ym.toString()); m.put("tariff", t.name); m.put("readings", rs.size());
        m.put("partialMonth", rs.size() < ym.lengthOfMonth() * 96);
        m.put("kwh", r1(kwh)); m.put("peakKwh", r1(peakKwh)); m.put("maxDemandKw", r1(maxKw));
        m.put("energyCharge", Math.round(energy)); m.put("demandCharge", Math.round(demand)); m.put("fixedCharge", Math.round(t.fixedCharge));
        m.put("tax", Math.round(tax)); m.put("estimatedTotalInr", Math.round(total));
        Optional<EnergyBill> actual = bills.findByBillingMonth(ym.toString());
        m.put("actualBillInr", actual.map(b -> b.actualAmount).orElse(null));
        m.put("variancePercent", actual.map(b -> pct(total, b.actualAmount)).orElse(null));
        List<Map<String, Object>> byDept = new ArrayList<>();
        Map<String, Double> byBuilding = new TreeMap<>();
        for (Department d : departments.findByStatus("ACTIVE")) {
            if (!meter.equals(d.meterCode)) continue;
            double c = total * d.allocationPercent / 100.0;
            Map<String, Object> x = new LinkedHashMap<>(); x.put("department", d.name); x.put("costInr", Math.round(c)); byDept.add(x);
            if (d.building != null) byBuilding.merge(d.building.name, c, Double::sum);
        }
        m.put("departmentCost", byDept);
        m.put("buildingCost", byBuilding.entrySet().stream().map(e -> { Map<String, Object> x = new LinkedHashMap<>(); x.put("building", e.getKey()); x.put("costInr", Math.round(e.getValue())); return x; }).collect(Collectors.toList()));
        m.put("note", "Department/building split uses the simulated allocation percentages (single-meter dataset).");
        return m;
    }

    public List<Map<String, Object>> monthlyComparison(String meter) {
        List<MeterReading> all = readings.range(meter, LocalDateTime.of(2000, 1, 1, 0, 0), an.now(meter));
        TreeSet<YearMonth> months = all.stream().map(r -> YearMonth.from(r.readingTime)).collect(Collectors.toCollection(TreeSet::new));
        List<Map<String, Object>> out = new ArrayList<>();
        Double prev = null;
        for (YearMonth ym : months) {
            Map<String, Object> e = estimate(meter, ym);
            double total = ((Number) e.get("estimatedTotalInr")).doubleValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.toString()); row.put("kwh", e.get("kwh")); row.put("estimatedTotalInr", e.get("estimatedTotalInr"));
            row.put("changePercent", prev == null ? null : pct(total, prev)); row.put("partialMonth", e.get("partialMonth"));
            prev = total; out.add(row);
        }
        List<Map<String, Object>> full = out.stream().filter(r -> !(boolean) r.get("partialMonth")).collect(Collectors.toList());
        if (!full.isEmpty()) {
            full.stream().max(Comparator.comparingDouble(r -> ((Number) r.get("estimatedTotalInr")).doubleValue())).ifPresent(r -> r.put("flag", "HIGHEST"));
            full.stream().min(Comparator.comparingDouble(r -> ((Number) r.get("estimatedTotalInr")).doubleValue())).ifPresent(r -> r.put("flag", "LOWEST"));
        }
        return out;
    }
}
