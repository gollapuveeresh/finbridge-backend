package com.finbridge.repository;

import com.finbridge.entity.OrganizationProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationProposalRepository extends JpaRepository<OrganizationProposal, UUID> {
    List<OrganizationProposal> findByOrganizationIdAndActiveTrue(UUID organizationId);
    List<OrganizationProposal> findByConsultantIdAndActiveTrue(UUID consultantId);
    List<OrganizationProposal> findByStatusAndActiveTrue(String status);
}
