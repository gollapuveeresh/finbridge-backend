package com.finbridge.repository;

import com.finbridge.entity.ConsultantPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConsultantPaymentRepository extends JpaRepository<ConsultantPayment, UUID> {
    List<ConsultantPayment> findByConsultantIdOrderByCreatedAtDesc(UUID consultantId);
    List<ConsultantPayment> findByStatusOrderByCreatedAtDesc(String status);
    List<ConsultantPayment> findByDepartmentOrderByCreatedAtDesc(String department);
    List<ConsultantPayment> findAllByOrderByCreatedAtDesc();
}
