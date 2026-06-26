package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue private UUID id;
    @Column(name = "invoice_number", unique = true) private String invoiceNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id", nullable = false) private User client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consultant_id", nullable = false) private User consultant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "proposal_id") private Proposal proposal;
    @Column(nullable = false) private String department;
    @Column(name = "service_title", nullable = false) private String serviceTitle;
    @Column(nullable = false) private BigDecimal subtotal;
    private BigDecimal tax = BigDecimal.ZERO;
    @Column(name = "tax_percent") private BigDecimal taxPercent = new BigDecimal("18");
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    private String currency = "INR";
    @Column(nullable = false) private String status = "draft";
    @Column(name = "due_date") private Instant dueDate;
    @Column(name = "paid_at") private Instant paidAt;
    private String notes;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();
    @Column(name = "is_active") private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
