package com.finbridge.repository;

import com.finbridge.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    List<ServiceRequest> findByOrganizationIdAndActiveTrue(UUID organizationId);
    List<ServiceRequest> findByDepartmentIdAndActiveTrue(String departmentId);
    List<ServiceRequest> findByConsultantIdAndActiveTrue(UUID consultantId);
    Optional<ServiceRequest> findByRequestNumber(String requestNumber);
    List<ServiceRequest> findByStatusAndActiveTrue(String status);
}
