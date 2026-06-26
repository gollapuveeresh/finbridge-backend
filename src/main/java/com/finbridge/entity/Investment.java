package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "investments")
public class Investment {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "investment_type", nullable = false) private String investmentType;
    @Column(name = "amount_invested", nullable = false) private BigDecimal amountInvested;
    @Column(name = "current_value", nullable = false) private BigDecimal currentValue;
    @Column(name = "purchase_date", nullable = false) private LocalDate purchaseDate;
    @Column(name = "risk_level", nullable = false) private String riskLevel;
    private String notes = "";
    @Column(name = "is_active") private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
