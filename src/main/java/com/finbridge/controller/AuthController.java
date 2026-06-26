package com.finbridge.controller;

import com.finbridge.dto.*;
import com.finbridge.entity.User;
import com.finbridge.security.SecurityRoles;
import com.finbridge.service.AuthService;
import com.finbridge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final DtoMapper mapper;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mapper.toUserResponse(user));
    }

    // ─── Email verification & password reset ─────────────────────────────────
    @PostMapping("/verify-email")
    @Operation(summary = "Verify an account email via token")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> body) {
        authService.verifyEmail(body.get("token"));
        return ResponseEntity.ok(Map.of("status", "success", "message", "Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String link = authService.forgotPassword(body.get("email"));
        Map<String, String> resp = new java.util.HashMap<>();
        resp.put("message", "If an account exists for that email, a reset link has been sent.");
        if (link != null) resp.put("previewUrl", link); // dev convenience when SMTP is not configured
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a token")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("status", "success", "message", "Password reset successfully"));
    }

    // ─── Consultants ─────────────────────────────────────────────────────────
    @GetMapping("/consultants")
    @PreAuthorize(SecurityRoles.STAFF)
    @Operation(summary = "List consultants, optionally filtered by department")
    public ResponseEntity<Map<String, Object>> consultants(
            @RequestParam(required = false) String department) {
        List<User> list = department != null
                ? userService.getConsultantsByDepartment(department)
                : userService.getConsultants();
        return ResponseEntity.ok(Map.of("consultants",
                list.stream().map(mapper::toStaffResponse).toList()));
    }

    @PostMapping("/create-consultant")
    @PreAuthorize(SecurityRoles.ADMIN_OR_DEPT)
    @Operation(summary = "Create a consultant")
    public ResponseEntity<Map<String, Object>> createConsultant(@RequestBody CreateStaffRequest req) {
        User u = userService.createStaff(req.name(), req.email(), req.password(),
                req.phone(), "consultant", req.department());
        return ResponseEntity.ok(Map.of("consultant", mapper.toStaffResponse(u)));
    }

    @PatchMapping("/consultants/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_DEPT)
    @Operation(summary = "Update a consultant (status, profile)")
    public ResponseEntity<Map<String, Object>> updateConsultant(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        User u = userService.updateStaff(id, (Boolean) body.get("isActive"),
                (String) body.get("name"), (String) body.get("phone"), (String) body.get("department"));
        return ResponseEntity.ok(Map.of("consultant", mapper.toStaffResponse(u)));
    }

    @DeleteMapping("/consultants/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_DEPT)
    @Operation(summary = "Deactivate a consultant")
    public ResponseEntity<Map<String, String>> deleteConsultant(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Consultant deactivated"));
    }

    // ─── Admins (platform administrators only) ───────────────────────────────
    @GetMapping("/admins")
    @PreAuthorize(SecurityRoles.ADMINS)
    @Operation(summary = "List admins, optionally filtered by role/department")
    public ResponseEntity<Map<String, Object>> admins(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(Map.of("admins",
                userService.getAdmins(role, department).stream().map(mapper::toStaffResponse).toList()));
    }

    @PostMapping("/create-admin")
    @PreAuthorize(SecurityRoles.ADMINS)
    @Operation(summary = "Create a CRM or department admin")
    public ResponseEntity<Map<String, Object>> createAdmin(@RequestBody CreateStaffRequest req) {
        String role = req.role() != null ? req.role() : "department-admin";
        User u = userService.createStaff(req.name(), req.email(), req.password(),
                req.phone(), role, req.department());
        return ResponseEntity.ok(Map.of("admin", mapper.toStaffResponse(u)));
    }

    @PatchMapping("/admins/{id}")
    @PreAuthorize(SecurityRoles.ADMINS)
    @Operation(summary = "Update an admin (status, profile)")
    public ResponseEntity<Map<String, Object>> updateAdmin(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {
        User u = userService.updateStaff(id, (Boolean) body.get("isActive"),
                (String) body.get("name"), (String) body.get("phone"), (String) body.get("department"));
        return ResponseEntity.ok(Map.of("admin", mapper.toStaffResponse(u)));
    }

    @DeleteMapping("/admins/{id}")
    @PreAuthorize(SecurityRoles.ADMINS)
    @Operation(summary = "Deactivate an admin")
    public ResponseEntity<Map<String, String>> deleteAdmin(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Admin deactivated"));
    }

    // ─── Clients ─────────────────────────────────────────────────────────────
    @GetMapping("/clients")
    @PreAuthorize(SecurityRoles.STAFF)
    @Operation(summary = "List all clients")
    public ResponseEntity<Map<String, Object>> clients() {
        return ResponseEntity.ok(Map.of("clients",
                userService.getClients().stream().map(mapper::toStaffResponse).toList()));
    }

    @GetMapping("/consultant/clients")
    @PreAuthorize(SecurityRoles.STAFF)
    @Operation(summary = "List clients for the authenticated consultant")
    public ResponseEntity<Map<String, Object>> consultantClients(@AuthenticationPrincipal User user) {
        List<User> clients;
        if (user != null && "consultant".equalsIgnoreCase(user.getRole()) && user.getDepartment() != null && !user.getDepartment().isBlank()) {
            clients = userService.getClientsByDepartment(user.getDepartment());
        } else {
            clients = userService.getClients();
        }
        return ResponseEntity.ok(Map.of("clients",
                clients.stream().map(mapper::toStaffResponse).toList()));
    }

    @GetMapping("/users")
    @PreAuthorize(SecurityRoles.ADMINS)
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserResponse>> allUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream().map(mapper::toUserResponse).toList());
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize(SecurityRoles.ADMINS)
    @Operation(summary = "Toggle user active status")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable UUID id,
                                                      @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(mapper.toUserResponse(userService.updateActive(id, body.get("isActive"))));
    }
}
