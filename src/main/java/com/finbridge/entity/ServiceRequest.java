package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "service_requests")
public class ServiceRequest {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "request_number", unique = true)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** loans | tax | insurance | investment | wealth */
    @Column(name = "department_id", nullable = false)
    private String departmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id")
    private User consultant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_admin_id")
    private User departmentAdmin;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priority = "medium";

    @Column(nullable = false)
    private String status = "submitted";

    @Column(name = "amount_involved")
    private BigDecimal amountInvolved;

    private String currency = "INR";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
