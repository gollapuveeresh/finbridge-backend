package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leads")
public class Lead {

    @Id @GeneratedValue private UUID id;
    @Column(name = "lead_id", unique = true) private String leadId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    private String phone;
    private BigDecimal income;
    private String requirement;
    private BigDecimal budget;
    @Column(nullable = false) private String source = "website_form";
    @Column(nullable = false) private String status = "new";
    @Column(nullable = false) private String priority = "warm";
    @Column(nullable = false) private int score = 0;
    private String department;
    @Column(name = "service_type") private String serviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_consultant")
    private User assignedConsultant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin")
    private User assignedAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_owner")
    private User crmOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_client_id")
    private User convertedClient;

    @Column(name = "follow_up_date") private Instant followUpDate;
    @Column(name = "last_contacted_at") private Instant lastContactedAt;
    @Column(columnDefinition = "text[]") private String[] tags = {};

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("addedAt ASC")
    private List<LeadNote> notes = new ArrayList<>();

    @Column(name = "is_active") private boolean active = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
