package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "organizations")
public class Organization {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    private String industry;
    private String gstin;
    private String cin;
    private String pan;

    @Column(name = "annual_turnover")
    private BigDecimal annualTurnover;

    @Column(name = "employee_count")
    private Integer employeeCount;

    private String address;
    private String city;
    private String state;
    private String pincode;
    private String website;

    @Column(columnDefinition = "text[]")
    private String[] services = {};

    @Column(nullable = false)
    private String status = "pending";

    @Column(name = "kyc_verified", nullable = false)
    private boolean kycVerified = false;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
