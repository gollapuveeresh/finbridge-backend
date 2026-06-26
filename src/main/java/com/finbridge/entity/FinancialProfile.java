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
@Entity @Table(name = "financial_profiles")
public class FinancialProfile {
    @Id @GeneratedValue private UUID id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_consultant") private User assignedConsultant;
    @Column(name = "annual_income") private BigDecimal annualIncome = BigDecimal.ZERO;
    @Column(name = "monthly_income") private BigDecimal monthlyIncome = BigDecimal.ZERO;
    @Column(name = "monthly_expenses") private BigDecimal monthlyExpenses = BigDecimal.ZERO;
    private BigDecimal savings = BigDecimal.ZERO;
    @Column(name = "emergency_fund") private BigDecimal emergencyFund = BigDecimal.ZERO;
    @Column(name = "credit_score") private int creditScore = 600;
    @Column(name = "total_loan_amount") private BigDecimal totalLoanAmount = BigDecimal.ZERO;
    @Column(name = "monthly_emi") private BigDecimal monthlyEmi = BigDecimal.ZERO;
    @Column(name = "business_name") private String businessName = "";
    @Column(name = "business_type") private String businessType = "";
    @Column(name = "annual_revenue") private BigDecimal annualRevenue = BigDecimal.ZERO;
    @Column(name = "annual_expenses") private BigDecimal annualExpenses = BigDecimal.ZERO;
    @Column(name = "years_in_business") private int yearsInBusiness = 0;
    @Column(name = "current_investments") private BigDecimal currentInvestments = BigDecimal.ZERO;
    @Column(name = "risk_tolerance") private String riskTolerance = "Medium";
    @Column(name = "investment_goals", columnDefinition = "text[]") private String[] investmentGoals = {};
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExistingLoan> existingLoans = new ArrayList<>();
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
