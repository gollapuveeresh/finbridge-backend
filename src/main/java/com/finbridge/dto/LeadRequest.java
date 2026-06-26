package com.finbridge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record LeadRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    String phone,
    BigDecimal income,
    String requirement,
    BigDecimal budget,
    String source,
    String department,
    String serviceType
) {}
