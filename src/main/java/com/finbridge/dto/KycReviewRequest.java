package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Validated payload for a KYC review decision. */
public record KycReviewRequest(
        @NotBlank(message = "status is required")
        @Pattern(regexp = "verified|rejected|pending", message = "status must be verified, rejected or pending")
        String status,

        // Free-text reason; required (enforced in the service) when status = rejected.
        @Size(max = 500, message = "note must be at most 500 characters")
        String note
) {}
