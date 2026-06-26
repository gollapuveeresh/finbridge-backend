package com.finbridge.repository;

import com.finbridge.entity.OrganizationMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationMeetingRepository extends JpaRepository<OrganizationMeeting, UUID> {
    List<OrganizationMeeting> findByOrganizationIdOrderByScheduledAtDesc(UUID organizationId);
    List<OrganizationMeeting> findByConsultantIdOrderByScheduledAtDesc(UUID consultantId);
}
