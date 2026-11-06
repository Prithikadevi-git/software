package com.datamint.energy.repository;

import com.datamint.energy.model.Alert;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByOrderByTriggeredAtDesc();
    List<Alert> findByStatusOrderByTriggeredAtDesc(String status);
    List<Alert> findTop3ByStatusOrderByTriggeredAtDesc(String status);
    Optional<Alert> findFirstByTypeAndStatusOrderByTriggeredAtDesc(String type, String status);
    long countByStatus(String status);
    boolean existsByTypeAndMeterCodeAndTriggeredAt(String type, String meterCode, LocalDateTime at);
}
