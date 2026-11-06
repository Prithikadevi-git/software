package com.datamint.energy.model;

import jakarta.persistence.*;

/** Module 12 - key/value application settings. */
@Entity @Table(name = "system_settings")
public class SystemSetting {
    @Id @Column(name = "setting_key") public String key;
    @Column(name = "setting_value", length = 500) public String value;
}
