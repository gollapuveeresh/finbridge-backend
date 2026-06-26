package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "email_notifications")
public class EmailNotification {
    @Id @GeneratedValue private UUID id;

    @Column(nullable = false) private String recipient;
    @Column(nullable = false, length = 100) private String type;
    @Column(name = "related_entity_id") private UUID relatedEntityId;
    @Column(length = 500) private String subject;
    @Column(nullable = false, length = 20) private String status = "sent";
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "sent_at") private Instant sentAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
