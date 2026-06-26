package com.finbridge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload for creating an invoice from the consultant/department portals. */
public record InvoiceRequest(
    UUID clientId,
    UUID consultantId,
    String department,
    String serviceTitle,
    List<LineItem> lineItems,
    BigDecimal taxPercent,
    // Date-only — the form's date picker sends a calendar date (YYYY-MM-DD), not a timestamp.
    LocalDate dueDate,
    String notes
) {
    public record LineItem(String description, BigDecimal amount) {}
}
