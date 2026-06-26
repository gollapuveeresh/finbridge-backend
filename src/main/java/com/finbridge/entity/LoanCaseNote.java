package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "loan_case_notes")
public class LoanCaseNote {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "loan_case_id", nullable = false) private LoanCase loanCase;
    @Column(nullable = false) private String text;
    @Column(name = "added_by") private String addedBy;
    @Column(name = "added_at") private Instant addedAt;
}
