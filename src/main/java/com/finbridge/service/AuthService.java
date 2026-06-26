package com.finbridge.service;

import com.finbridge.dto.LoginRequest;
import com.finbridge.dto.LoginResponse;
import com.finbridge.dto.RegisterRequest;
import com.finbridge.entity.User;
import com.finbridge.exception.BadRequestException;
import com.finbridge.exception.UnauthorizedException;
import com.finbridge.repository.UserRepository;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final OrganizationUserRepository orgUserRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final long VERIFY_TTL_MS = 24 * 60 * 60 * 1000L; // 24h
    private static final long RESET_TTL_MS = 60 * 60 * 1000L; // 1h

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : "client");
        user.setDepartment(request.department());
        user.setPhone(request.phone());
        user.setCompanyName(request.companyName());
        user.setEmailVerified(false);

        User saved = userRepository.save(user);

        // Send verification email
        String token = jwtService.generatePurposeToken(saved.getId().toString(), "verify-email", VERIFY_TTL_MS);
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        emailService.sendVerificationEmail(saved.getEmail(), saved.getName(), verifyLink);
        log.info("User registered: {} role={}, verification email sent", saved.getEmail(), saved.getRole());

        return new LoginResponse(null, saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.getDepartment());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException(
                    "Please verify your email before logging in. Check your inbox for the verification link.");
        }
        log.info("User logged in: {}", user.getEmail());
        return toLoginResponse(user);
    }

    /** Verifies an email-verification token and marks the user verified. */
    @Transactional
    public void verifyEmail(String token) {
        String userId = jwtService.verifyPurposeToken(token, "verify-email");
        if (userId != null) {
            User user = userRepository.findById(java.util.UUID.fromString(userId))
                    .orElseThrow(() -> new BadRequestException("Account not found"));
            user.setEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified: {}", user.getEmail());
            return;
        }

        String orgUserId = jwtService.verifyPurposeToken(token, "verify-email-b2b");
        if (orgUserId != null) {
            com.finbridge.entity.OrganizationUser orgUser = orgUserRepository.findById(java.util.UUID.fromString(orgUserId))
                    .orElseThrow(() -> new BadRequestException("B2B Account not found"));
            orgUser.setEmailVerified(true);
            orgUser.setActive(true);
            orgUserRepository.save(orgUser);
            log.info("B2B Email verified: {}", orgUser.getEmail());
            return;
        }

        throw new BadRequestException("Invalid or expired verification link");
    }

    /**
     * Generates a password-reset link for the given email. Always succeeds from the
     * caller's perspective (no account enumeration). Returns the reset link so the
     * dev environment can surface a preview when SMTP isn't configured.
     */
    public String forgotPassword(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(user -> {
            String token = jwtService.generatePurposeToken(user.getId().toString(), "reset-password", RESET_TTL_MS);
            String link = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordReset(user.getEmail(), link);
            log.info("Password reset requested: {}", user.getEmail());
            return link;
        }).orElse(null);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String userId = jwtService.verifyPurposeToken(token, "reset-password");
        if (userId == null)
            throw new BadRequestException("Invalid or expired reset link");
        if (newPassword == null || newPassword.length() < 6)
            throw new BadRequestException("Password must be at least 6 characters");
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BadRequestException("Account not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset: {}", user.getEmail());
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getRole(), user.getDepartment());
    }
}
