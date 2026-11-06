package com.datamint.energy.repository;

import com.datamint.energy.model.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByStatus(String status);
}
