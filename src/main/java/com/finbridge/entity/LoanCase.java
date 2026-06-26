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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "loan_cases")
public class LoanCase {
    @Id @GeneratedValue private UUID id;
    @Column(name = "case_id", unique = true) private String caseId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id", nullable = false) private User client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consultant_id", nullable = false) private User consultant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id") private Lead lead;

    @Column(nullable = false) private String stage = "document_collection";
    @Column(name = "loan_type") private String loanType = "";
    @Column(name = "requested_amount") private BigDecimal requestedAmount = BigDecimal.ZERO;
    @Column(name = "approved_amount") private BigDecimal approvedAmount;
    @Column(name = "interest_rate") private BigDecimal interestRate;
    @Column(name = "tenure_months") private Integer tenureMonths;
    @Column(name = "monthly_emi") private BigDecimal monthlyEmi;
    @Column(name = "bank_name") private String bankName = "";
    @Column(name = "disbursed_date") private LocalDate disbursedDate;
    @Column(name = "disbursed_amount") private BigDecimal disbursedAmount;

    // Eligibility
    @Column(name = "credit_score") private Integer creditScore;
    private BigDecimal dti;
    private BigDecimal ltv;
    private Boolean eligible;
    @Column(name = "analyst_note") private String analystNote = "";

    // Recommendation
    @Column(name = "recommended_bank") private String recommendedBank = "";
    @Column(name = "recommended_rate") private BigDecimal recommendedRate;
    @Column(name = "recommended_tenure") private Integer recommendedTenure;
    @Column(name = "recommended_emi") private BigDecimal recommendedEmi;
    @Column(name = "recommendation_note") private String recommendationNote = "";
    @Column(name = "sent_to_client") private Boolean sentToClient = false;

    // Client decision
    @Column(name = "client_decision") private String clientDecision = "Pending";
    @Column(name = "client_feedback") private String clientFeedback = "";
    @Column(name = "decided_at") private Instant decidedAt;

    // Bank processing
    @Column(name = "application_ref") private String applicationRef = "";
    @Column(name = "submitted_date") private LocalDate submittedDate;
    @Column(name = "bank_status") private String bankStatus = "Not Submitted";
    @Column(name = "sanctioned_at") private Instant sanctionedAt;
    @Column(name = "bank_remarks") private String bankRemarks = "";

    @Column(name = "proposal_id") private UUID proposalId;
    @Column(name = "invoice_id") private UUID invoiceId;

    @OneToMany(mappedBy = "loanCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LoanCaseDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "loanCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("dueDate ASC")
    private List<EmiScheduleItem> emiSchedule = new ArrayList<>();

    @OneToMany(mappedBy = "loanCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("addedAt ASC")
    private List<LoanCaseNote> notes = new ArrayList<>();

    @Column(name = "is_active", nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
