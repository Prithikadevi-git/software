package com.datamint.energy.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Module 9 - Equipment management */
@Entity @Table(name = "equipment")
public class Equipment implements Deactivatable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String tag;
    @Column(nullable = false) public String name;
    public String category;
    @ManyToOne public Department department;
    public String meterCode;
    public Double ratedKw;
    public String status = "RUNNING";
    public LocalDate lastMaintenance;
    @Column(length = 500) public String maintenanceNotes;
    public void deactivate() { status = "RETIRED"; }
}
