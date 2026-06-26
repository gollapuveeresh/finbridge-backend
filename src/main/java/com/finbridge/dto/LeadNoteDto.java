package com.finbridge.dto;

import java.time.Instant;

/** A single CRM note attached to a lead. */
public record LeadNoteDto(String text, String addedBy, Instant addedAt) {}
