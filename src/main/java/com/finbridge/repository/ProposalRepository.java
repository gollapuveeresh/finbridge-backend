package com.finbridge.repository;

import com.finbridge.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {
    List<Proposal> findByClientIdAndActiveTrue(UUID clientId);
    List<Proposal> findByConsultantIdAndActiveTrue(UUID consultantId);
    Page<Proposal> findByClientIdAndActiveTrue(UUID clientId, Pageable pageable);
    Page<Proposal> findByConsultantIdAndActiveTrue(UUID consultantId, Pageable pageable);

    List<Proposal> findByClientIdAndActiveTrueOrderByCreatedAtDesc(UUID clientId);
    List<Proposal> findByConsultantIdAndActiveTrueOrderByCreatedAtDesc(UUID consultantId);
    List<Proposal> findByDepartmentAndActiveTrueOrderByCreatedAtDesc(String department);
    List<Proposal> findByActiveTrueOrderByCreatedAtDesc();

    List<Proposal> findByCaseIdAndCaseModelAndActiveTrue(UUID caseId, String caseModel);
}
