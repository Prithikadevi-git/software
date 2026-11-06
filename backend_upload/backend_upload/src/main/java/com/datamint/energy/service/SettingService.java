package com.datamint.energy.service;

import com.datamint.energy.model.SystemSetting;
import com.datamint.energy.repository.SystemSettingRepository;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/** Module 12 - System Settings */
@Service
public class SettingService {
    private final SystemSettingRepository repo;
    public SettingService(SystemSettingRepository repo) { this.repo = repo; }

    public String get(String key, String def) { return repo.findById(key).map(s -> s.value).orElse(def); }
    public double num(String key, double def) {
        try { return Double.parseDouble(get(key, String.valueOf(def))); } catch (Exception e) { return def; }
    }
    public Map<String, String> all() {
        Map<String, String> m = new TreeMap<>();
        repo.findAll().forEach(s -> m.put(s.key, s.value));
        return m;
    }
    public void put(String key, String value) {
        SystemSetting s = new SystemSetting(); s.key = key; s.value = value; repo.save(s);
    }
}
