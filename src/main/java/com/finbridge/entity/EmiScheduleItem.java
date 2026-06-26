package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "emi_schedule")
public class EmiScheduleItem {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "loan_case_id", nullable = false) private LoanCase loanCase;
    @Column(nullable = false) private String month;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "paid_date") private LocalDate paidDate;
    private String status = "Pending";
    private BigDecimal penalty = BigDecimal.ZERO;
}
