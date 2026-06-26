package com.finbridge.repository;

import com.finbridge.entity.LoanCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoanCaseRepository extends JpaRepository<LoanCase, UUID> {
    List<LoanCase> findByActiveTrueOrderByCreatedAtDesc();
    List<LoanCase> findByConsultantIdAndActiveTrueOrderByCreatedAtDesc(UUID consultantId);
    List<LoanCase> findByClientIdAndActiveTrueOrderByCreatedAtDesc(UUID clientId);
}
