package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;

public record ProposalDecisionRequest(
    @NotBlank String status,   // approved | changes_requested | rejected
    String feedback
) {}
