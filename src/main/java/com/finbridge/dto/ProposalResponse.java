package com.finbridge.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Proposal shape consumed by the frontend (nested leadId/clientId/consultantId refs). */
public record ProposalResponse(
    UUID id,
    Ref leadId,
    Ref clientId,
    Ref consultantId,
    String department,
    String title,
    String summary,
    Map<String, Object> details,
    String status,
    String clientFeedback,
    Instant validUntil,
    Instant createdAt
) {
    public record Ref(UUID id, String name) {}
}
