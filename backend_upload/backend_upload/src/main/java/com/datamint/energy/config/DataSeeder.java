package com.datamint.energy.config;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import com.datamint.energy.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** First start: creates sample master data, imports the 6-month CSV, and scans it for alerts. */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final String SQL = "insert into meter_readings(meter_code,reading_time,usage_kwh,lag_kvarh,lead_kvarh,co2_tco2,lag_pf,lead_pf,nsm,week_status,day_of_week,load_type) values (?,?,?,?,?,?,?,?,?,?,?,?)";
    private final AppUserRepository users; private final BuildingRepository buildings; private final DepartmentRepository departments;
    private final MeterRepository meters; private final EquipmentRepository equipment; private final TariffRepository tariffs;
    private final MeterReadingRepository readings; private final AlertRepository alerts;
    private final SettingService settings; private final AlertService alertService;
    private final JdbcTemplate jdbc; private final ResourceLoader loader;
    @Value("${app.dataset.path}") private String path;

    public DataSeeder(AppUserRepository users, BuildingRepository buildings, DepartmentRepository departments, MeterRepository meters,
                      EquipmentRepository equipment, TariffRepository tariffs, MeterReadingRepository readings, AlertRepository alerts,
                      SettingService settings, AlertService alertService, JdbcTemplate jdbc, ResourceLoader loader) {
        this.users = users; this.buildings = buildings; this.departments = departments; this.meters = meters; this.equipment = equipment;
        this.tariffs = tariffs; this.readings = readings; this.alerts = alerts; this.settings = settings; this.alertService = alertService;
        this.jdbc = jdbc; this.loader = loader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedMaster();
        importCsv();
        if (alerts.count() == 0) System.out.println("Alerts generated: " + alertService.scan(AnalyticsService.DEFAULT_METER, 30));
    }

    private void user(String u, String pw, String name, Role r) {
        AppUser a = new AppUser(); a.username = u; a.passwordHash = new BCryptPasswordEncoder().encode(pw); a.fullName = name; a.role = r; users.save(a);
    }

    private Building building(String code, String name, String type, int floors, double area) {
        Building b = new Building(); b.code = code; b.name = name; b.type = type; b.floors = floors; b.areaSqft = area; b.location = "Bengaluru"; return buildings.save(b);
    }

    private Department dept(String name, Building b, double pct, double target) {
        Department d = new Department(); d.name = name; d.building = b; d.meterCode = AnalyticsService.DEFAULT_METER;
        d.allocationPercent = pct; d.monthlyTargetKwh = target; return departments.save(d);
    }

    private void eq(String tag, String name, String cat, Department d, double kw) {
        Equipment e = new Equipment(); e.tag = tag; e.name = name; e.category = cat; e.department = d; e.meterCode = AnalyticsService.DEFAULT_METER;
        e.ratedKw = kw; e.lastMaintenance = LocalDate.of(2026, 9, 15); equipment.save(e);
    }

    private void seedMaster() {
        if (users.count() == 0) {
            user("admin", "admin123", "John Smith", Role.ADMIN);
            user("manager", "manager123", "Mary Manager", Role.MANAGER);
            user("engineer", "engineer123", "Eric Engineer", Role.ENGINEER);
            user("viewer", "viewer123", "Vera Viewer", Role.VIEWER);
        }
        if (buildings.count() == 0) {
            Building main = building("MB01", "Main Building", "Office", 4, 20000), prod = building("PU01", "Production Unit", "Production", 2, 50000);
            Department p = dept("Production", prod, 45, 37000), h = dept("HVAC", main, 25, 21000);
            dept("Lighting", main, 15, 12500); dept("Others", main, 15, 12500);
            eq("EQ-001", "Rolling Mill Motor", "Motor", p, 250); eq("EQ-002", "Arc Furnace", "Furnace", p, 400);
            eq("EQ-003", "Chiller Unit", "HVAC", h, 120); eq("EQ-004", "Air Compressor", "Compressor", p, 90);
            Meter m = new Meter(); m.code = AnalyticsService.DEFAULT_METER; m.name = "Main Incomer Meter"; m.building = main; meters.save(m);
        }
        if (tariffs.count() == 0) {
            Tariff t = new Tariff(); t.name = "Industrial Time-of-Day (sample)"; t.energyRate = 8; t.peakRate = 10; t.peakStartHour = 9; t.peakEndHour = 18;
            t.demandRatePerKw = 300; t.fixedCharge = 25000; t.taxPercent = 5; tariffs.save(t);
        }
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("company.name", "DATAMINT AARVIX"); defaults.put("units.energy", "kWh"); defaults.put("currency", "INR");
        defaults.put("timezone", "Asia/Kolkata"); defaults.put("alert.highKwh", "100"); defaults.put("alert.lowPf", "70");
        defaults.put("alert.anomalyZ", "3"); defaults.put("refresh.seconds", "15"); defaults.put("target.monthlyKwh", "80000");
        defaults.put("carbon.targetTco2PerMonth", "300"); defaults.put("meter.dataSource", "SIMULATED_HISTORICAL_REPLAY");
        defaults.forEach((k, v) -> { if (settings.get(k, null) == null) settings.put(k, v); });
    }

    private void importCsv() throws IOException {
        if (readings.count() > 0) return;
        Resource res = loader.getResource(path);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        int n = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            br.readLine();   // header
            List<Object[]> batch = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = line.split(",");
                batch.add(new Object[]{c[1].trim(), Timestamp.valueOf(LocalDateTime.parse(c[0].trim(), f)), Double.parseDouble(c[3]), Double.parseDouble(c[4]),
                        Double.parseDouble(c[5]), Double.parseDouble(c[6]), Double.parseDouble(c[7]), Double.parseDouble(c[8]), Integer.parseInt(c[9].trim()),
                        c[10].trim(), c[11].trim(), c[12].trim()});
                if (batch.size() == 2000) { jdbc.batchUpdate(SQL, batch); n += batch.size(); batch.clear(); }
            }
            if (!batch.isEmpty()) { jdbc.batchUpdate(SQL, batch); n += batch.size(); }
        }
        System.out.println("Imported " + n + " meter readings.");
    }
}
