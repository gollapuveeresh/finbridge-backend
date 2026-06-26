package com.finbridge.repository;

import com.finbridge.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {
    List<Consultation> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    List<Consultation> findByConsultantIdOrderByCreatedAtDesc(UUID consultantId);
    List<Consultation> findByDepartmentOrderByCreatedAtDesc(String department);
    List<Consultation> findAllByOrderByCreatedAtDesc();
    List<Consultation> findByClientEmailInOrderByCreatedAtDesc(List<String> emails);
}
