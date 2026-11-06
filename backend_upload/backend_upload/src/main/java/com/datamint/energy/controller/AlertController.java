package com.datamint.energy.controller;

import com.datamint.energy.model.Alert;
import com.datamint.energy.repository.AlertRepository;
import com.datamint.energy.service.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Module 6 - alerts: history, acknowledge, resolve, assign, scan. */
@RestController @RequestMapping("/api/alerts")
public class AlertController {
    private final AlertRepository repo; private final AlertService service;
    public AlertController(AlertRepository repo, AlertService service) { this.repo = repo; this.service = service; }

    @GetMapping public List<Alert> list(@RequestParam(required = false) String status) {
        return status == null ? repo.findAllByOrderByTriggeredAtDesc() : repo.findByStatusOrderByTriggeredAtDesc(status);
    }

    private Alert find(Long id) { return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")); }

    @PutMapping("/{id}/acknowledge") public Alert ack(@PathVariable Long id) { Alert a = find(id); a.status = "ACKNOWLEDGED"; return repo.save(a); }
    @PutMapping("/{id}/resolve") public Alert resolve(@PathVariable Long id) { Alert a = find(id); a.status = "RESOLVED"; return repo.save(a); }
    @PutMapping("/{id}/assign") public Alert assign(@PathVariable Long id, @RequestParam String user) { Alert a = find(id); a.assignedTo = user; return repo.save(a); }

    @PostMapping("/scan") public Map<String, Object> scan(@RequestParam(defaultValue = AnalyticsService.DEFAULT_METER) String meter, @RequestParam(defaultValue = "7") int days) {
        return Map.of("newAlerts", service.scan(meter, days));
    }
}
