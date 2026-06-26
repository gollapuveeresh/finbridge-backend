package com.finbridge.controller;

import com.finbridge.dto.KycReviewRequest;
import com.finbridge.entity.OrganizationDocument;
import com.finbridge.entity.User;
import com.finbridge.security.SecurityRoles;
import com.finbridge.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Compliance review of client (organization) documents")
@PreAuthorize(SecurityRoles.STAFF)
public class KycController {

    private final KycService kycService;

    @GetMapping("/documents")
    @Operation(summary = "Review queue: all organization documents with status")
    public ResponseEntity<Map<String, Object>> queue() {
        return ResponseEntity.ok(Map.of("documents", kycService.reviewQueue()));
    }

    @PatchMapping("/documents/{docId}")
    @Operation(summary = "Verify or reject a document; recomputes the org's KYC status")
    public ResponseEntity<Map<String, Object>> review(@PathVariable UUID docId,
                                                      @Valid @RequestBody KycReviewRequest req,
                                                      @AuthenticationPrincipal User reviewer) {
        return ResponseEntity.ok(Map.of("document",
                kycService.review(docId, req.status(), req.note(), reviewer)));
    }

    @GetMapping("/documents/{docId}/file")
    @Operation(summary = "Download a document for review")
    public ResponseEntity<Map<String, String>> file(@PathVariable UUID docId) {
        OrganizationDocument d = kycService.getDocument(docId);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("fileName", d.getFileName());
        out.put("content", d.getFileUrl());
        return ResponseEntity.ok(out);
    }
}
