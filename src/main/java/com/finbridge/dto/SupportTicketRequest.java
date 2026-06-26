package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Validated payload for raising a B2B support ticket. */
public record SupportTicketRequest(
        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject is too long")
        String subject,

        @NotBlank(message = "Description is required")
        String description,

        String category,
        String priority
) {}
