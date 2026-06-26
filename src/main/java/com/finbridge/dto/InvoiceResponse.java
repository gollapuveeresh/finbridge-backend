package com.finbridge.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Invoice shape consumed by the frontend (nested clientId/consultantId refs). */
public record InvoiceResponse(
    UUID id,
    String invoiceNumber,
    Ref clientId,
    Ref consultantId,
    String serviceTitle,
    String department,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal taxPercent,
    BigDecimal totalAmount,
    String status,
    Instant dueDate,
    Instant paidAt,
    String notes,
    List<LineItem> lineItems,
    Instant createdAt
) {
    public record Ref(UUID id, String name) {}
    public record LineItem(String description, BigDecimal amount) {}
}
