package com.datamint.energy.controller;

import com.datamint.energy.service.SettingService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Module 12 - System settings. */
@RestController @RequestMapping("/api/settings")
public class SettingController {
    private final SettingService settings;
    public SettingController(SettingService settings) { this.settings = settings; }

    @GetMapping public Map<String, String> all() { return settings.all(); }

    @PutMapping public Map<String, String> update(@RequestBody Map<String, String> values) {
        values.forEach(settings::put); return settings.all();
    }
}
