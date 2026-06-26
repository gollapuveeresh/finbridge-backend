package com.finbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "consultant_payments")
public class ConsultantPayment {
    @Id @GeneratedValue private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "consultant_id", nullable = false) 
    private User consultant;
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "consultation_id", nullable = false) 
    private Consultation consultation;
    
    @Column(name = "client_name", nullable = false)
    private String clientName;
    
    @Column(nullable = false)
    private String department;
    
    @Column(name = "fee_amount", nullable = false)
    private BigDecimal feeAmount;
    
    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount;
    
    @Column(nullable = false)
    private String status = "pending"; // pending | paid
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @CreationTimestamp 
    @Column(name = "created_at", updatable = false) 
    private Instant createdAt;
    
    @UpdateTimestamp 
    @Column(name = "updated_at") 
    private Instant updatedAt;
}
