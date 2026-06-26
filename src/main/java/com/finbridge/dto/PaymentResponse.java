package com.finbridge.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payment shape consumed by the frontend (nested clientId/invoiceId refs). */
public record PaymentResponse(
    UUID id,
    Ref clientId,
    InvoiceRef invoiceId,
    BigDecimal amount,
    String gateway,
    String method,
    String status,
    Instant createdAt
) {
    public record Ref(UUID id, String name) {}
    public record InvoiceRef(UUID id, String invoiceNumber) {}
}
