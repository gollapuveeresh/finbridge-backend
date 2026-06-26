package com.finbridge.controller;

import com.finbridge.dto.PaymentRequest;
import com.finbridge.entity.User;
import com.finbridge.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.finbridge.security.SecurityRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment tracking & revenue")
@PreAuthorize(SecurityRoles.STAFF)
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "List payments")
    public ResponseEntity<Map<String, Object>> getAll() {
        return ResponseEntity.ok(Map.of("payments", paymentService.getAll()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Payment revenue stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of("stats", paymentService.getStats()));
    }

    @PostMapping
    @Operation(summary = "Record a payment against an invoice and mark it paid")
    public ResponseEntity<Map<String, Object>> create(@RequestBody PaymentRequest req,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("payment", paymentService.create(req, user)));
    }
}
