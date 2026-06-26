package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "payments")
public class Payment {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id", nullable = false) private User client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id", nullable = false) private Invoice invoice;
    @Column(nullable = false) private BigDecimal amount;
    private String currency = "INR";
    private String gateway = "razorpay";
    @Column(name = "gateway_order_id") private String gatewayOrderId;
    @Column(name = "gateway_payment_id") private String gatewayPaymentId;
    @Column(name = "gateway_signature") private String gatewaySignature;
    @Column(nullable = false) private String status = "created";
    private String method = "";
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "refund_id") private String refundId;
    @Column(name = "refunded_at") private Instant refundedAt;
    @Column(name = "refund_amount") private BigDecimal refundAmount;
    private String notes;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
