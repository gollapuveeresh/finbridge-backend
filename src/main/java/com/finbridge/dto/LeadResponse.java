package com.finbridge.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LeadResponse(
    UUID id,
    String leadId,
    String name,
    String email,
    String phone,
    BigDecimal income,
    String requirement,
    BigDecimal budget,
    String source,
    String status,
    String priority,
    int score,
    String department,
    String serviceType,
    UUID assignedConsultantId,
    String assignedConsultantName,
    UUID convertedClientId,
    List<LeadNoteDto> notes,
    Instant followUpDate,
    boolean isActive,
    Instant createdAt
) {}
