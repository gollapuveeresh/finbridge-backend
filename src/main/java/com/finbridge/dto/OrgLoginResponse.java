package com.finbridge.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class OrgLoginResponse {
    private String token;
    private UUID orgUserId;
    private UUID organizationId;
    private String companyName;
    private String industry;
    private String gstin;
    private String status;
    private boolean kycVerified;
    private String userName;
    private String email;
    private String role;
}
