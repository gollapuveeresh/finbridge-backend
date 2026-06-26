package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanRequest(
    @NotBlank String loanNumber,
    @NotBlank String loanType,
    @NotBlank String lenderName,
    @NotNull @Positive BigDecimal principalAmount,
    @NotNull @Positive BigDecimal outstandingBalance,
    @NotNull @Positive BigDecimal interestRate,
    @NotNull @Positive int tenureMonths,
    @NotNull @Positive BigDecimal monthlyEmi,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String notes
) {}
