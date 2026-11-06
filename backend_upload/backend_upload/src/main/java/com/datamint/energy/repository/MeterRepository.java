package com.datamint.energy.repository;

import com.datamint.energy.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterRepository extends JpaRepository<Meter, Long> {}
