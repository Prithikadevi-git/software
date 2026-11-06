package com.datamint.energy.model;

import jakarta.persistence.*;

/** Module 5 - Electricity tariff (time-of-day: peak window uses peakRate). */
@Entity @Table(name = "tariffs")
public class Tariff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public double energyRate = 8;        // INR per kWh (off-peak)
    public double peakRate = 8;          // INR per kWh (peak window)
    public int peakStartHour = 9;
    public int peakEndHour = 18;
    public double demandRatePerKw = 0;   // INR per kW of monthly maximum demand
    public double fixedCharge = 0;       // INR per month
    public double taxPercent = 0;
    public boolean active = true;
}
