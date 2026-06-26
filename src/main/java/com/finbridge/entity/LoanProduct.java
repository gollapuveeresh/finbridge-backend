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
@Entity @Table(name = "loan_products")
public class LoanProduct {
    @Id @GeneratedValue private UUID id;
    @Column(name = "bank_name", nullable = false) private String bankName;
    @Column(name = "loan_type", nullable = false) private String loanType;
    @Column(name = "min_credit_score", nullable = false) private int minCreditScore;
    @Column(name = "max_credit_score", nullable = false) private int maxCreditScore;
    @Column(name = "min_monthly_income", nullable = false) private BigDecimal minMonthlyIncome;
    @Column(name = "max_loan_amount", nullable = false) private BigDecimal maxLoanAmount;
    @Column(name = "interest_rate", nullable = false) private BigDecimal interestRate;
    @Column(name = "processing_fee", nullable = false) private BigDecimal processingFee;
    @Column(name = "tenure_months", nullable = false) private int tenureMonths;
    private String description = "";
    @Column(columnDefinition = "text[]") private String[] features = {};
    @Column(name = "eligibility_criteria") private String eligibilityCriteria = "";
    @Column(name = "bank_logo") private String bankLogo = "";
    @Column(name = "official_website") private String officialWebsite = "";
    @Column(name = "pre_approved") private boolean preApproved = false;
    private boolean featured = false;
    @Column(name = "is_active") private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
