package com.finbridge.repository;

import com.finbridge.entity.OrganizationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationDocumentRepository extends JpaRepository<OrganizationDocument, UUID> {
    List<OrganizationDocument> findByOrganizationId(UUID organizationId);
    List<OrganizationDocument> findByOrganizationIdAndStatus(UUID organizationId, String status);
    List<OrganizationDocument> findByStatus(String status);
}
