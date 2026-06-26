package com.finbridge.repository;

import com.finbridge.entity.DeptCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeptCaseRepository extends JpaRepository<DeptCase, UUID> {
    List<DeptCase> findByDepartmentAndActiveTrueOrderByCreatedAtDesc(String department);
}
