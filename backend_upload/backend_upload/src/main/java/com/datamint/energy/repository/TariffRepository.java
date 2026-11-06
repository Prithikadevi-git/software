package com.datamint.energy.repository;

import com.datamint.energy.model.Tariff;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    Optional<Tariff> findFirstByActiveTrue();
}
