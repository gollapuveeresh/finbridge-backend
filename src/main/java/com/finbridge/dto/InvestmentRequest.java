package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentRequest(
    @NotBlank String investmentType,
    @NotNull @Positive BigDecimal amountInvested,
    @NotNull @Positive BigDecimal currentValue,
    @NotNull LocalDate purchaseDate,
    @NotBlank String riskLevel,
    String notes
) {}
