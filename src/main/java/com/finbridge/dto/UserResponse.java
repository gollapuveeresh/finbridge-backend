package com.finbridge.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String role,
    String department,
    String phone,
    String companyName,
    boolean active,
    boolean emailVerified,
    Instant createdAt
) {}
