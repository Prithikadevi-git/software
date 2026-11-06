package com.datamint.energy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Module 11 - User & Access Management */
@Entity @Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String username;
    @JsonIgnore public String passwordHash;
    @Transient @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password;
    public String fullName;
    @Enumerated(EnumType.STRING) public Role role = Role.VIEWER;
    public boolean active = true;
    public String allowedBuildings;      // comma separated building ids, empty = all
    public LocalDateTime lastLogin;
}
