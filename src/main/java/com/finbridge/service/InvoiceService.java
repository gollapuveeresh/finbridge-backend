package com.finbridge.service;

import com.finbridge.dto.InvoiceRequest;
import com.finbridge.dto.InvoiceResponse;
import com.finbridge.entity.Invoice;
import com.finbridge.entity.InvoiceLineItem;
import com.finbridge.entity.OrganizationPayment;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.InvoiceRepository;
import com.finbridge.repository.OrganizationPaymentRepository;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final SequenceGenerator sequenceGenerator;
    private final PaymentService paymentService;
    private final OrganizationUserRepository organizationUserRepository;
    private final OrganizationPaymentRepository organizationPaymentRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getByDepartment(String department) {
        return invoiceRepository.findByDepartmentAndActiveTrueOrderByCreatedAtDesc(department)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getOne(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest req, User actingConsultant) {
        User client = userRepository.findById(req.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + req.clientId()));
        User consultant = req.consultantId() != null
                ? userRepository.findById(req.consultantId()).orElse(actingConsultant)
                : actingConsultant;

        Invoice inv = new Invoice();
        inv.setInvoiceNumber(sequenceGenerator.next(SequenceGenerator.Seq.INVOICE));
        inv.setClient(client);
        inv.setConsultant(consultant);
        inv.setDepartment(req.department());
        inv.setServiceTitle(req.serviceTitle());
        inv.setStatus("draft");
        // Entity stores an Instant; the request carries a calendar date. Convert at the boundary.
        inv.setDueDate(req.dueDate() == null ? null
                : req.dueDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        inv.setNotes(req.notes());

        BigDecimal taxPercent = req.taxPercent() != null ? req.taxPercent() : new BigDecimal("18");
        BigDecimal subtotal = BigDecimal.ZERO;
        if (req.lineItems() != null) {
            for (InvoiceRequest.LineItem li : req.lineItems()) {
                InvoiceLineItem item = new InvoiceLineItem();
                item.setInvoice(inv);
                item.setDescription(li.description());
                item.setAmount(li.amount());
                inv.getLineItems().add(item);
                subtotal = subtotal.add(li.amount() != null ? li.amount() : BigDecimal.ZERO);
            }
        }
        BigDecimal tax = subtotal.multiply(taxPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        inv.setSubtotal(subtotal);
        inv.setTaxPercent(taxPercent);
        inv.setTax(tax);
        inv.setTotalAmount(subtotal.add(tax));

        Invoice saved = invoiceRepository.save(inv);
        log.info("Invoice created: {} total={}", saved.getInvoiceNumber(), saved.getTotalAmount());
        // Mirror to the client's B2B portal if the client is an organization user (so they can pay it).
        syncToOrgPayment(saved);

        // Email the client about the new invoice
        if (client.getEmail() != null) {
            emailService.sendPaymentRequested(client.getEmail(), client.getName(),
                    saved.getInvoiceNumber(), saved.getTotalAmount(), saved.getDueDate(), saved.getId());
        }

        // Notify consultant and department admins when invoice is created
        try {
            String title = "Invoice Generated";
            String message = "A new invoice " + saved.getInvoiceNumber() + " has been generated for client " + saved.getClient().getName() + 
                    " for amount ₹" + saved.getTotalAmount() + ".";
            
            // 1. Notify consultant
            if (saved.getConsultant() != null) {
                notificationService.create(saved.getConsultant(), "invoice", title, message);
            }
            
            // 2. Notify department admins
            for (User admin : userRepository.findByRoleAndDepartmentAndActiveTrue("department-admin", saved.getDepartment())) {
                notificationService.create(admin, "invoice", title, message + " (Dept: " + saved.getDepartment() + ")");
            }
        } catch (Exception e) {
            log.error("Failed to notify staff of invoice generation: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public InvoiceResponse updateStatus(UUID id, String status, User actor) {
        Invoice inv = load(id);
        OwnershipGuard.assertConsultantOwns(actor, inv.getConsultant(), "invoice");
        boolean newlyPaid = "paid".equals(status) && !"paid".equals(inv.getStatus());
        inv.setStatus(status);
        if ("paid".equals(status) && inv.getPaidAt() == null) inv.setPaidAt(Instant.now());
        Invoice saved = invoiceRepository.save(inv);
        // Keep revenue analytics in sync: settling an invoice records a payment (idempotent).
        if (newlyPaid) paymentService.recordForInvoice(saved, "manual");
        // Propagate the status change to the client's B2B portal payment (pending → paid).
        syncToOrgPayment(saved);
        return toResponse(saved);
    }

    /**
     * If the invoice's client is also a B2B organization user, mirror the invoice into the org's
     * portal as an OrganizationPayment so it appears under Payments & Invoices. Idempotent — keyed
     * on the invoice number, so status changes update the same row instead of duplicating it.
     */
    private void syncToOrgPayment(Invoice inv) {
        User client = inv.getClient();
        if (client == null || client.getEmail() == null) return;
        organizationUserRepository.findByEmailIgnoreCase(client.getEmail()).ifPresent(orgUser -> {
            UUID orgId = orgUser.getOrganization().getId();
            OrganizationPayment op = organizationPaymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                    .filter(e -> inv.getInvoiceNumber() != null && inv.getInvoiceNumber().equals(e.getGatewayOrderId()))
                    .findFirst()
                    .orElseGet(OrganizationPayment::new);
            if (op.getPaymentNumber() == null)
                op.setPaymentNumber(sequenceGenerator.next(SequenceGenerator.Seq.ORG_PAYMENT));
            op.setOrganization(orgUser.getOrganization());
            op.setGateway("invoice");
            op.setGatewayOrderId(inv.getInvoiceNumber());   // dedup key + shown as the gateway ref
            op.setAmount(inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
            op.setStatus(mapInvoiceStatus(inv.getStatus()));
            if ("paid".equals(op.getStatus()) && op.getPaidAt() == null)
                op.setPaidAt(inv.getPaidAt() != null ? inv.getPaidAt() : Instant.now());
            organizationPaymentRepository.save(op);
            log.info("Synced invoice {} to organization {} as a B2B payment ({})",
                    inv.getInvoiceNumber(), orgId, op.getStatus());
        });
    }

    private String mapInvoiceStatus(String invoiceStatus) {
        if (invoiceStatus == null) return "pending";
        return switch (invoiceStatus) {
            case "paid" -> "paid";
            case "cancelled" -> "cancelled";
            default -> "pending";   // draft / sent / overdue → still owed
        };
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        List<Invoice> all = invoiceRepository.findByActiveTrueOrderByCreatedAtDesc();
        BigDecimal totalInvoiced = BigDecimal.ZERO, totalPaid = BigDecimal.ZERO;
        long overdueCount = 0;
        Map<String, BigDecimal> deptRevenue = new LinkedHashMap<>();
        Map<String, Long> deptCount = new LinkedHashMap<>();
        Instant now = Instant.now();

        for (Invoice inv : all) {
            BigDecimal amt = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            totalInvoiced = totalInvoiced.add(amt);
            if ("paid".equals(inv.getStatus())) totalPaid = totalPaid.add(amt);
            boolean pastDue = inv.getDueDate() != null && inv.getDueDate().isBefore(now);
            if ("overdue".equals(inv.getStatus()) || (pastDue && !"paid".equals(inv.getStatus()) && !"cancelled".equals(inv.getStatus())))
                overdueCount++;
            String dept = inv.getDepartment() != null ? inv.getDepartment() : "unassigned";
            deptRevenue.merge(dept, amt, BigDecimal::add);
            deptCount.merge(dept, 1L, Long::sum);
        }

        List<Map<String, Object>> byDepartment = deptRevenue.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("_id", e.getKey());
            m.put("revenue", e.getValue());
            m.put("count", deptCount.get(e.getKey()));
            return m;
        }).toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalInvoiced", totalInvoiced);
        stats.put("totalPaid", totalPaid);
        stats.put("overdueCount", overdueCount);
        stats.put("byDepartment", byDepartment);
        return stats;
    }

    private Invoice load(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private InvoiceResponse toResponse(Invoice inv) {
        InvoiceResponse.Ref client = inv.getClient() != null
                ? new InvoiceResponse.Ref(inv.getClient().getId(), inv.getClient().getName()) : null;
        InvoiceResponse.Ref consultant = inv.getConsultant() != null
                ? new InvoiceResponse.Ref(inv.getConsultant().getId(), inv.getConsultant().getName()) : null;
        List<InvoiceResponse.LineItem> items = inv.getLineItems().stream()
                .map(li -> new InvoiceResponse.LineItem(li.getDescription(), li.getAmount())).toList();
        return new InvoiceResponse(
                inv.getId(), inv.getInvoiceNumber(), client, consultant,
                inv.getServiceTitle(), inv.getDepartment(), inv.getSubtotal(), inv.getTax(),
                inv.getTaxPercent(), inv.getTotalAmount(), inv.getStatus(),
                inv.getDueDate(), inv.getPaidAt(), inv.getNotes(), items, inv.getCreatedAt()
        );
    }
}
