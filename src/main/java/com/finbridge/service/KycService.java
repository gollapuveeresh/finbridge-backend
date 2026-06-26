package com.finbridge.service;

import com.finbridge.entity.Organization;
import com.finbridge.entity.OrganizationDocument;
import com.finbridge.entity.User;
import com.finbridge.exception.BadRequestException;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.OrganizationDocumentRepository;
import com.finbridge.repository.OrganizationRepository;
import com.finbridge.repository.OrganizationUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Compliance / KYC review workflow. Staff review the documents a B2B client uploaded and mark each
 * verified or rejected; once every required document is verified the organization's KYC flips to
 * verified (which gates downstream service delivery, the way a Big-4 firm gates an engagement).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    /** Document types that must all be verified for an organization to be KYC-complete. */
    private static final Set<String> REQUIRED =
            Set.of("GST_CERTIFICATE", "PAN", "CIN", "INCORPORATION", "BANK_STATEMENT");

    private final OrganizationDocumentRepository docRepo;
    private final OrganizationRepository orgRepo;
    private final OrganizationUserRepository orgUserRepo;
    private final EmailService emailService;

    /** All uploaded organization documents awaiting / completed review, newest first. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> reviewQueue() {
        return docRepo.findAll().stream()
                .sorted(Comparator.comparing(OrganizationDocument::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDocument getDocument(UUID docId) {
        return docRepo.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    /** Verify or reject a document, then recompute the organization's KYC status. */
    @Transactional
    public Map<String, Object> review(UUID docId, String status, String note, User reviewer) {
        if (!Set.of("verified", "rejected", "pending").contains(status))
            throw new BadRequestException("status must be verified, rejected or pending");
        if ("rejected".equals(status) && (note == null || note.isBlank()))
            throw new BadRequestException("A rejection reason is required");
        OrganizationDocument d = getDocument(docId);
        d.setStatus(status);
        d.setReviewerNote("rejected".equals(status) ? note : null);
        d.setReviewedBy(reviewer);
        d.setReviewedAt(Instant.now());
        docRepo.save(d);

        Organization org = d.getOrganization();
        recomputeKyc(org);
        log.info("KYC doc {} ({}) marked {} by {}", d.getDocumentType(), org.getCompanyName(), status,
                reviewer != null ? reviewer.getEmail() : "system");

        // Notify organization users via email
        try {
            if (org != null) {
                var users = orgUserRepo.findByOrganizationId(org.getId());
                for (com.finbridge.entity.OrganizationUser u : users) {
                    if (u.getEmail() != null && !u.getEmail().isBlank()) {
                        String subject = "KYC Document Status Update — " + d.getDocumentType();
                        String statusColor = "verified".equals(status) ? "#22c55e" : "#ef4444";
                        String msgHtml = "Your KYC document <strong>" + d.getDocumentType() + "</strong> (" + (d.getFileName() != null ? d.getFileName() : "") + ") has been marked as <strong style='color:" + statusColor + "'>" + status + "</strong>" +
                                ("rejected".equals(status) ? ". Reason: " + note : ".") + 
                                "<br/><br/>Current Organization KYC Status: <strong>" + (org.isKycVerified() ? "VERIFIED ✓" : "PENDING") + "</strong>";
                        
                        String body = emailService.buildNotificationEmailBody(u.getName(), subject, msgHtml);
                        emailService.sendHtml(u.getEmail(), subject + " | FinBridge", body, "kyc_status", null);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send KYC review email notifications for organization: {}", org != null ? org.getCompanyName() : "unknown", e);
        }

        return toDto(d);
    }

    /** An org is KYC-verified when every REQUIRED document type has a verified upload. */
    private void recomputeKyc(Organization org) {
        List<OrganizationDocument> docs = docRepo.findByOrganizationId(org.getId());
        boolean complete = REQUIRED.stream().allMatch(type ->
                docs.stream().anyMatch(x -> type.equals(x.getDocumentType()) && "verified".equals(x.getStatus())));
        org.setKycVerified(complete);
        org.setStatus(complete ? "verified" : "pending");
        orgRepo.save(org);
    }

    private Map<String, Object> toDto(OrganizationDocument d) {
        Organization org = d.getOrganization();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("_id", d.getId());
        m.put("organizationId", org != null ? org.getId() : null);
        m.put("companyName", org != null ? org.getCompanyName() : "Organization");
        m.put("documentType", d.getDocumentType());
        m.put("required", REQUIRED.contains(d.getDocumentType()));
        m.put("fileName", d.getFileName());
        m.put("status", d.getStatus());
        m.put("reviewerNote", d.getReviewerNote());
        m.put("reviewedAt", d.getReviewedAt());
        m.put("createdAt", d.getCreatedAt());
        m.put("kycVerified", org != null && org.isKycVerified());
        return m;
    }
}
