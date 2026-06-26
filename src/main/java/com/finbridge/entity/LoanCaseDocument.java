package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "loan_case_documents")
public class LoanCaseDocument {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "loan_case_id", nullable = false) private LoanCase loanCase;
    @Column(nullable = false) private String name;
    private String category = "Other";
    private String status = "Pending";
    @Column(name = "uploaded_at") private Instant uploadedAt;
    @Column(name = "rejection_note") private String rejectionNote = "";
}
