package com.datamint.energy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Module 6 - alerts (including usage anomalies). status: OPEN, ACKNOWLEDGED, RESOLVED */
@Entity @Table(name = "alerts", indexes = @Index(name = "ix_alert_time", columnList = "triggeredAt"))
public class Alert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String type, severity, title, meterCode;
    @Column(length = 500) public String message;
    public LocalDateTime triggeredAt;
    public String status = "OPEN";
    public String assignedTo;
    public Double readingValue;
}
