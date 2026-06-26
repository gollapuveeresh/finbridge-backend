package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "loans")
public class Loan {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "loan_number", nullable = false, unique = true) private String loanNumber;
    @Column(name = "loan_type", nullable = false) private String loanType;
    @Column(name = "lender_name", nullable = false) private String lenderName;
    @Column(name = "principal_amount", nullable = false) private BigDecimal principalAmount;
    @Column(name = "outstanding_balance", nullable = false) private BigDecimal outstandingBalance;
    @Column(name = "interest_rate", nullable = false) private BigDecimal interestRate;
    @Column(name = "tenure_months", nullable = false) private int tenureMonths;
    @Column(name = "monthly_emi", nullable = false) private BigDecimal monthlyEmi;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(nullable = false) private String status = "Active";
    private String notes = "";
    @Column(name = "is_active") private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
