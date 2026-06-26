package com.finbridge.repository;

import com.finbridge.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<SupportTicket> findByStatus(String status);
    List<SupportTicket> findByAssignedToId(UUID assignedToId);
}
