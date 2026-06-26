package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "organization_documents")
public class OrganizationDocument {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private OrganizationUser uploadedBy;

    /** GST_CERTIFICATE | PAN | CIN | INCORPORATION | BANK_STATEMENT | FINANCIAL_STATEMENT | OTHER */
    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "file_name")
    private String fileName;

    // Stored as a base64 data URI. @JsonIgnore so the (potentially large) content is never shipped
    // in list responses — it is served only via the dedicated download endpoint.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    /** pending | verified | rejected */
    @Column(nullable = false)
    private String status = "pending";

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
