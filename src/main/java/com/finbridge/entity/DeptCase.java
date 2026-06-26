package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A department service case (tax / investment / insurance / wealth). The stage-specific,
 * polymorphic workflow data is stored in a single JSONB {@code data} column so each
 * department can carry its own structures without separate schemas.
 */
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "dept_cases")
public class DeptCase {
    @Id @GeneratedValue private UUID id;
    @Column(name = "case_id", unique = true) private String caseId;
    @Column(nullable = false) private String department;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") private User client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consultant_id") private User consultant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id") private Lead lead;

    @Column(nullable = false) private String stage = "document_collection";
    @Column(name = "invoice_id") private UUID invoiceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> data = new HashMap<>();

    @Column(name = "is_active", nullable = false) private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
