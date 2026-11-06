package com.datamint.energy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String username, action, path;
    public LocalDateTime loggedAt = LocalDateTime.now();
}
