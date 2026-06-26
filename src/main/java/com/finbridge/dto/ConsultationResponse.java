package com.finbridge.dto;

import java.time.Instant;
import java.util.UUID;

/** Consultation shape consumed by the frontend (nested clientId/consultantId refs). */
public record ConsultationResponse(
    UUID id,
    Ref clientId,
    Ref consultantId,
    String department,
    String category,
    String status,
    String clientNotes,
    String confirmedDate,
    String confirmedTime,
    String meetingLink,
    Instant createdAt,
    Boolean recordingEnabled,
    String videoUrl
) {
    public record Ref(UUID id, String name, String email, String companyName) {}
}
