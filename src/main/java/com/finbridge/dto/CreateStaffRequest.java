package com.finbridge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Payload for creating an admin or consultant from the admin portals. */
public record CreateStaffRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank String password,
    String phone,
    String role,        // crm-admin | department-admin | consultant (defaulted by endpoint)
    String department
) {}
