package com.datamint.energy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Modules 2 & 4 - one 15-minute reading. Column names are explicit because DataSeeder inserts with JDBC. */
@Entity @Table(name = "meter_readings", indexes = @Index(name = "ix_meter_time", columnList = "meter_code,reading_time"))
public class MeterReading {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "meter_code", nullable = false, length = 30) public String meterCode;
    @Column(name = "reading_time", nullable = false) public LocalDateTime readingTime;
    @Column(name = "usage_kwh") public double usageKwh;
    @Column(name = "lag_kvarh") public double lagKvarh;
    @Column(name = "lead_kvarh") public double leadKvarh;
    @Column(name = "co2_tco2") public double co2Tco2;
    @Column(name = "lag_pf") public double lagPf = 100;
    @Column(name = "lead_pf") public double leadPf = 100;
    @Column(name = "nsm") public int nsm;
    @Column(name = "week_status", length = 20) public String weekStatus;
    @Column(name = "day_of_week", length = 20) public String dayOfWeek;
    @Column(name = "load_type", length = 20) public String loadType;
}
