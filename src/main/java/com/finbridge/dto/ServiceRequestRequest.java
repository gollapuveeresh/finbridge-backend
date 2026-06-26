package com.finbridge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ServiceRequestRequest {
    @NotBlank(message = "Please choose a department")
    private String departmentId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
    private String priority;
    private BigDecimal amountInvolved;
    private String currency;
    private String notes;
}
