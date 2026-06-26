package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;

/** Validated payload for uploading a KYC document (content is a base64 data URI). */
public record DocumentUploadRequest(
        @NotBlank(message = "documentType is required")
        String documentType,

        @NotBlank(message = "fileName is required")
        String fileName,

        @NotBlank(message = "file content is required")
        String content
) {}
