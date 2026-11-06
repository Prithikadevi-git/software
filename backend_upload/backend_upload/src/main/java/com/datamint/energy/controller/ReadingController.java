package com.datamint.energy.controller;

import com.datamint.energy.model.MeterReading;
import com.datamint.energy.repository.MeterReadingRepository;
import com.datamint.energy.service.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Modules 2 - real-time monitoring and electricity-usage history. */
@RestController @RequestMapping("/api/readings")
public class ReadingController {
    private final MeterReadingRepository repo; private final AlertService alerts; private final AnalyticsService analytics;
    public ReadingController(MeterReadingRepository repo, AlertService alerts, AnalyticsService analytics) { this.repo = repo; this.alerts = alerts; this.analytics = analytics; }

    @GetMapping
    public Map<String, Object> history(@RequestParam(defaultValue = AnalyticsService.DEFAULT_METER) String meter,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        Pageable p = PageRequest.of(page, Math.min(size, 1000));
        Page<MeterReading> pg = (from != null && to != null)
                ? repo.findByMeterCodeAndReadingTimeBetweenOrderByReadingTimeDesc(meter, from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusSeconds(1), p)
                : repo.findByMeterCodeOrderByReadingTimeDesc(meter, p);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content", pg.getContent()); m.put("page", page); m.put("totalElements", pg.getTotalElements()); m.put("totalPages", pg.getTotalPages());
        return m;
    }

    @GetMapping("/latest")
    public List<MeterReading> latest(@RequestParam(defaultValue = AnalyticsService.DEFAULT_METER) String meter, @RequestParam(defaultValue = "10") int limit) {
        return repo.findByMeterCodeOrderByReadingTimeDesc(meter, PageRequest.of(0, Math.min(limit, 500))).getContent();
    }

    /** Receive a reading from a meter gateway. */
    @PostMapping
    public MeterReading create(@RequestBody MeterReading r) {
        if (r.meterCode == null || r.readingTime == null || r.usageKwh < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meterCode, readingTime and usageKwh (>=0) are required");
        r.id = null; MeterReading saved = repo.save(r); alerts.check(saved); return saved;
    }

    @PostMapping("/bulk")
    public Map<String, Object> bulk(@RequestBody List<MeterReading> list) {
        list.forEach(r -> r.id = null); repo.saveAll(list); list.forEach(alerts::check);
        return Map.of("saved", list.size());
    }

    /** Missing 15-minute readings in the last N days. */
    @GetMapping("/missing")
    public Map<String, Object> missing(@RequestParam(defaultValue = AnalyticsService.DEFAULT_METER) String meter, @RequestParam(defaultValue = "30") int days) {
        List<MeterReading> w = analytics.window(meter, days, 0);
        long missing = 0; List<String> gaps = new ArrayList<>();
        for (int i = 1; i < w.size(); i++) {
            long mins = Duration.between(w.get(i - 1).readingTime, w.get(i).readingTime).toMinutes();
            if (mins > 15) { missing += mins / 15 - 1; if (gaps.size() < 50) gaps.add(w.get(i - 1).readingTime + " -> " + w.get(i).readingTime); }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("meter", meter); m.put("days", days); m.put("readings", w.size()); m.put("missingIntervals", missing); m.put("gaps", gaps);
        return m;
    }
}
