package com.datamint.energy.model;

import jakarta.persistence.*;

/** Module 3 - Department. allocationPercent = simulated share of the meter's energy (single-meter dataset). */
@Entity @Table(name = "departments")
public class Department implements Deactivatable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String name;
    @ManyToOne public Building building;
    public String meterCode;
    public double allocationPercent;
    public Double monthlyTargetKwh;
    public String status = "ACTIVE";
    public void deactivate() { status = "INACTIVE"; }
}
