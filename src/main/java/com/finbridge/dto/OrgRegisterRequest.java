package com.finbridge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrgRegisterRequest {
    @NotBlank(message = "Company name is required")
    private String companyName;
    private String industry;
    @Size(max = 20, message = "GSTIN looks too long")
    private String gstin;
    private String cin;
    private String pan;
    private BigDecimal annualTurnover;
    private Integer employeeCount;
    private String address;
    private String city;
    private String state;
    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "Enter a valid 6-digit pincode")
    private String pincode;
    private String website;
    private List<String> services;

    // Primary contact / admin user
    @NotBlank(message = "Contact name is required")
    private String adminName;
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String adminEmail;
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;
    // Lenient: accepts plain 10-digit, +91 prefix, spaces, hyphens, parentheses.
    @Pattern(regexp = "^$|^[+]?[0-9 ()\\-]{7,20}$", message = "Enter a valid phone number")
    private String adminPhone;
}
