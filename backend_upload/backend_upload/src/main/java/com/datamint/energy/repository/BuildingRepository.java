package com.datamint.energy.repository;

import com.datamint.energy.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {}
