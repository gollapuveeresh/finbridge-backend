package com.finbridge.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Records a payment against an invoice. Amount defaults to the invoice total when omitted.
 * No external gateway is integrated — this captures a manual/offline settlement.
 */
public record PaymentRequest(
        UUID invoiceId,
        BigDecimal amount,
        String method,
        String gateway,
        String notes
) {}
