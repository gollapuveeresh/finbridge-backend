package com.finbridge.repository;

import com.finbridge.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByClientIdAndActiveTrueOrderByCreatedAtDesc(UUID clientId);
    List<Invoice> findByConsultantIdAndActiveTrueOrderByCreatedAtDesc(UUID consultantId);
    List<Invoice> findByActiveTrueOrderByCreatedAtDesc();
    List<Invoice> findByDepartmentAndActiveTrueOrderByCreatedAtDesc(String department);
    long countByActiveTrue();
}
