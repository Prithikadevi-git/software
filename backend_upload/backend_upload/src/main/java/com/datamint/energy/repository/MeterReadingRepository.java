package com.datamint.energy.repository;

import com.datamint.energy.model.MeterReading;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    /** readings in the window (from, to] ordered by time */
    @Query("select r from MeterReading r where r.meterCode=:m and r.readingTime>:f and r.readingTime<=:t order by r.readingTime")
    List<MeterReading> range(@Param("m") String m, @Param("f") LocalDateTime f, @Param("t") LocalDateTime t);

    @Query("select max(r.readingTime) from MeterReading r where r.meterCode=:m")
    LocalDateTime latest(@Param("m") String m);

    Page<MeterReading> findByMeterCodeOrderByReadingTimeDesc(String meterCode, Pageable p);

    Page<MeterReading> findByMeterCodeAndReadingTimeBetweenOrderByReadingTimeDesc(String meterCode, LocalDateTime a, LocalDateTime b, Pageable p);
}
