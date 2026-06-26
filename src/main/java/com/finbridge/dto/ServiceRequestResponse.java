package com.finbridge.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ServiceRequestResponse {
    private UUID id;
    private String requestNumber;
    private UUID organizationId;
    private String companyName;
    private String departmentId;
    private String consultantName;
    private String title;
    private String description;
    private String priority;
    private String status;
    private BigDecimal amountInvolved;
    private String currency;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private String meetingStatus;
    private String meetingDate;
    private String meetingTime;
    private String meetingLink;
}
