package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultationRequest(
    @NotBlank String department,
    @NotBlank String category,
    String clientNotes
) {}
