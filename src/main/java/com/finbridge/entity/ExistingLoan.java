package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "existing_loans")
public class ExistingLoan {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "profile_id", nullable = false) private FinancialProfile profile;
    @Column(name = "loan_type", nullable = false) private String loanType;
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "monthly_payment") private BigDecimal monthlyPayment = BigDecimal.ZERO;
}
