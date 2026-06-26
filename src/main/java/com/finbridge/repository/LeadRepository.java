package com.finbridge.repository;

import com.finbridge.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    List<Lead> findByActiveTrueOrderByCreatedAtDesc();
    List<Lead> findByDepartmentAndActiveTrue(String department);
    Optional<Lead> findByLeadId(String leadId);
    List<Lead> findByEmailIgnoreCase(String email);

    Page<Lead> findByActiveTrue(Pageable pageable);
    Page<Lead> findByDepartmentAndActiveTrue(String department, Pageable pageable);
    Page<Lead> findByStatusAndActiveTrue(String status, Pageable pageable);
    Page<Lead> findByDepartmentAndStatusAndActiveTrue(String department, String status, Pageable pageable);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.active = true AND l.status = :status")
    long countByStatus(String status);
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.active = true")
    long countActive();
}
