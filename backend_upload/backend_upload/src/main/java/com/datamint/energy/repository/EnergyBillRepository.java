package com.datamint.energy.repository;

import com.datamint.energy.model.EnergyBill;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnergyBillRepository extends JpaRepository<EnergyBill, Long> {
    Optional<EnergyBill> findByBillingMonth(String month);
}
