package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "consultations")
public class Consultation {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id", nullable = false) private User client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consultant_id") private User consultant;
    @Column(nullable = false) private String department;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private String status = "pending";
    @Column(name = "client_notes") private String clientNotes = "";
    @Column(name = "confirmed_date") private String confirmedDate = "";
    @Column(name = "confirmed_time") private String confirmedTime = "";
    @Column(name = "meeting_link") private String meetingLink = "";
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
