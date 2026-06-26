package com.finbridge.repository;

import com.finbridge.entity.OrganizationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationPaymentRepository extends JpaRepository<OrganizationPayment, UUID> {
    List<OrganizationPayment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<OrganizationPayment> findByStatus(String status);
}
