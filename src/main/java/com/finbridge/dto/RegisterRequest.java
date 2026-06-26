package com.finbridge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
    @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
    String role,
    String department,
    @Pattern(regexp = "^$|^[+]?[0-9 ()\\-]{7,20}$", message = "Enter a valid phone number") String phone,
    String companyName
) {}
