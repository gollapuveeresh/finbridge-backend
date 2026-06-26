package com.finbridge.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Partial update payload for a lead used by the CRM portal. All fields are
 * optional — only non-null values are applied — so the CRM can patch a single
 * attribute (e.g. status or department) without resending the whole record.
 */
public record LeadUpdateRequest(
    String name,
    String email,
    String phone,
    BigDecimal income,
    String requirement,
    BigDecimal budget,
    String source,
    String department,
    String serviceType,
    String status,
    String priority,
    Integer score,
    java.util.UUID assignedConsultant,
    Instant followUpDate
) {}
