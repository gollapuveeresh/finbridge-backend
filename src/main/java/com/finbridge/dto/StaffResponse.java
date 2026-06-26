package com.finbridge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Response shape for staff/user listings (clients, consultants, admins) consumed
 * by the React frontend. Boolean JSON keys are explicitly named `isActive` /
 * `isEmailVerified` to match the legacy contract the frontend reads.
 */
public record StaffResponse(
    UUID id,
    String name,
    String email,
    String phone,
    String role,
    String department,
    String companyName,
    @JsonProperty("isActive") boolean active,
    @JsonProperty("isEmailVerified") boolean emailVerified,
    Instant createdAt
) {}
