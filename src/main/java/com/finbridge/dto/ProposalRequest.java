package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record ProposalRequest(
    UUID leadId,
    UUID clientId,
    @NotNull UUID consultantId,
    @NotBlank String department,
    @NotBlank String title,
    String summary,
    Map<String, Object> details,
    // Date-only — the proposal form sends a calendar date (YYYY-MM-DD), not a timestamp.
    LocalDate validUntil
) {}
