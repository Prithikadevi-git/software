package com.datamint.energy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Module 3 - Building management */
@Entity @Table(name = "buildings")
public class Building implements Deactivatable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    @Column(nullable = false) public String name;
    public String location, type;
    public Integer floors;
    public Double areaSqft;
    public String status = "ACTIVE";
    public String description;
    public LocalDateTime createdAt = LocalDateTime.now();
    public void deactivate() { status = "INACTIVE"; }
}
