package com.datamint.energy.model;

import jakarta.persistence.*;

@Entity @Table(name = "meters")
public class Meter implements Deactivatable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String code;
    public String name;
    @ManyToOne public Building building;
    public String status = "ACTIVE";
    public String dataSource = "SIMULATED";
    public void deactivate() { status = "INACTIVE"; }
}
