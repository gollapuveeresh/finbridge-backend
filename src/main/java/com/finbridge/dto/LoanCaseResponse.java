package com.finbridge.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Loan-case shape consumed by the frontend loan workflow (nested sub-objects assembled from flat columns). */
public record LoanCaseResponse(
    UUID id,
    String caseId,
    Ref clientId,
    Ref consultantId,
    String loanType,
    BigDecimal requestedAmount,
    String stage,
    List<DocumentDto> documents,
    Eligibility eligibility,
    Recommendation recommendation,
    ClientDecision clientDecision,
    BankProcessing bankProcessing,
    UUID invoiceId,
    BigDecimal disbursedAmount,
    LocalDate disbursedDate,
    BigDecimal interestRate,
    Integer tenureMonths,
    BigDecimal monthlyEMI,
    String bankName,
    List<EmiDto> emiSchedule,
    List<NoteDto> notes,
    Instant createdAt
) {
    public record Ref(UUID id, String name) {}
    public record DocumentDto(UUID id, String name, String category, String status,
                              String rejectionNote, Instant uploadedAt) {}
    public record Eligibility(Integer creditScore, BigDecimal dti, BigDecimal ltv,
                              Boolean eligible, String analystNote) {}
    public record Recommendation(String recommendedBank, BigDecimal recommendedRate,
                                 Integer recommendedTenure, BigDecimal recommendedEMI,
                                 String note, Boolean sentToClient) {}
    public record ClientDecision(String status, Instant decidedAt, String feedback) {}
    public record BankProcessing(String applicationRef, LocalDate submittedDate,
                                 Instant sanctionedAt, String status, String remarks) {}
    public record EmiDto(UUID id, String month, LocalDate dueDate, BigDecimal amount,
                         BigDecimal penalty, LocalDate paidDate, String status) {}
    public record NoteDto(String text, String addedBy, Instant addedAt) {}
}
