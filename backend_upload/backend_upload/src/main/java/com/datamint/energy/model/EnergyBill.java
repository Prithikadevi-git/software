package com.datamint.energy.model;

import jakarta.persistence.*;

/** Module 5 - actual bill entry, used to compare against the calculated estimate. */
@Entity @Table(name = "energy_bills")
public class EnergyBill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String billingMonth;   // e.g. 2026-10
    public double actualAmount;
    public Double kwhBilled;
    public String notes;
}
