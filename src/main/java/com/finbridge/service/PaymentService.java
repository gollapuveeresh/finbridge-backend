package com.finbridge.service;

import com.finbridge.dto.PaymentRequest;
import com.finbridge.dto.PaymentResponse;
import com.finbridge.entity.Invoice;
import com.finbridge.entity.Payment;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.InvoiceRepository;
import com.finbridge.repository.PaymentRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final List<String> SUCCESS_STATUSES = List.of("paid", "captured", "success");

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    /**
     * Explicitly record a payment against an invoice and mark the invoice paid.
     * No external gateway — captures a manual/offline settlement.
     */
    @Transactional
    public PaymentResponse create(PaymentRequest req, User actor) {
        if (req == null || req.invoiceId() == null)
            throw new com.finbridge.exception.BadRequestException("invoiceId is required");
        Invoice invoice = invoiceRepository.findById(req.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + req.invoiceId()));
        OwnershipGuard.assertConsultantOwns(actor, invoice.getConsultant(), "invoice");
        BigDecimal amount = req.amount() != null ? req.amount() : invoice.getTotalAmount();
        Payment payment = recordPayment(invoice, amount,
                req.method() != null ? req.method() : "manual",
                req.gateway() != null ? req.gateway() : "manual",
                req.notes());
        markInvoicePaid(invoice);
        return toResponse(payment);
    }

    /**
     * Idempotently record the settlement for an invoice that has been marked paid.
     * Called by the invoice workflow so revenue analytics stays in sync without a separate gateway.
     */
    @Transactional
    public Payment recordForInvoice(Invoice invoice, String method) {
        for (Payment existing : paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoice.getId()))
            if (SUCCESS_STATUSES.contains(existing.getStatus())) return existing; // already settled
        return recordPayment(invoice, invoice.getTotalAmount(),
                method != null ? method : "manual", "manual", null);
    }

    private Payment recordPayment(Invoice invoice, BigDecimal amount, String method, String gateway, String notes) {
        Payment p = new Payment();
        p.setInvoice(invoice);
        p.setClient(invoice.getClient());
        p.setAmount(amount != null ? amount : BigDecimal.ZERO);
        p.setStatus("paid");
        p.setMethod(method);
        p.setGateway(gateway);
        p.setPaidAt(Instant.now());
        p.setNotes(notes);
        Payment saved = paymentRepository.save(p);
        log.info("Payment recorded: invoice={} amount={}", invoice.getInvoiceNumber(), saved.getAmount());

        // Email the client about successful payment
        if (invoice.getClient() != null && invoice.getClient().getEmail() != null) {
            emailService.sendPaymentCompleted(invoice.getClient().getEmail(), invoice.getClient().getName(),
                    invoice.getInvoiceNumber(), saved.getAmount(), saved.getId());
        }

        // Notify consultant, department admins, and CRM admins about payment
        try {
            String title = "Payment Confirmed";
            String message = "Client " + invoice.getClient().getName() + " has paid invoice " + invoice.getInvoiceNumber() + 
                    " of amount ₹" + saved.getAmount() + ".";
            
            // 1. Notify consultant
            if (invoice.getConsultant() != null) {
                notificationService.create(invoice.getConsultant(), "payment", title, message);
            }
            
            // 2. Notify department admins
            for (User admin : userRepository.findByRoleAndDepartmentAndActiveTrue("department-admin", invoice.getDepartment())) {
                notificationService.create(admin, "payment", title, message + " (Dept: " + invoice.getDepartment() + ")");
            }
            
            // 3. Notify CRM admins
            for (User crmAdmin : userRepository.findByRoleAndActiveTrue("crm-admin")) {
                notificationService.create(crmAdmin, "payment", title, message);
            }
        } catch (Exception e) {
            log.error("Failed to generate payment notifications: {}", e.getMessage());
        }

        return saved;
    }

    private void markInvoicePaid(Invoice invoice) {
        if (!"paid".equals(invoice.getStatus())) {
            invoice.setStatus("paid");
            invoice.setPaidAt(Instant.now());
            invoiceRepository.save(invoice);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        List<Payment> all = paymentRepository.findAllByOrderByCreatedAtDesc();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long successCount = 0;
        // Group by calendar month (1-12) to match the frontend chart's MONTHS[_id.month - 1] lookup.
        Map<Integer, BigDecimal> monthRevenue = new java.util.TreeMap<>();
        Map<Integer, Long> monthCount = new java.util.TreeMap<>();

        for (Payment p : all) {
            boolean success = SUCCESS_STATUSES.contains(p.getStatus());
            if (!success) continue;
            successCount++;
            BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(amt);
            int month = p.getCreatedAt() != null
                    ? LocalDate.ofInstant(p.getCreatedAt(), ZoneId.systemDefault()).getMonthValue()
                    : 0;
            monthRevenue.merge(month, amt, BigDecimal::add);
            monthCount.merge(month, 1L, Long::sum);
        }

        List<Map<String, Object>> monthlyRevenue = monthRevenue.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("_id", Map.of("month", e.getKey()));
            m.put("revenue", e.getValue());
            m.put("count", monthCount.get(e.getKey()));
            return m;
        }).toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalTransactions", successCount);
        stats.put("monthlyRevenue", monthlyRevenue);
        return stats;
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse.Ref client = p.getClient() != null
                ? new PaymentResponse.Ref(p.getClient().getId(), p.getClient().getName()) : null;
        PaymentResponse.InvoiceRef invoice = p.getInvoice() != null
                ? new PaymentResponse.InvoiceRef(p.getInvoice().getId(), p.getInvoice().getInvoiceNumber()) : null;
        return new PaymentResponse(
                p.getId(), client, invoice, p.getAmount(),
                p.getGateway(), p.getMethod(), p.getStatus(), p.getCreatedAt()
        );
    }
}
