package com.finbridge.controller;

import com.finbridge.dto.InvoiceRequest;
import com.finbridge.entity.User;
import com.finbridge.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.finbridge.security.SecurityRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management & revenue")
@PreAuthorize(SecurityRoles.STAFF)
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "List invoices, optionally by department")
    public ResponseEntity<Map<String, Object>> getAll(@RequestParam(required = false) String department) {
        return ResponseEntity.ok(Map.of("invoices",
                department != null ? invoiceService.getByDepartment(department) : invoiceService.getAll()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Invoice revenue stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of("stats", invoiceService.getStats()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one invoice")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("invoice", invoiceService.getOne(id)));
    }

    @PostMapping
    @Operation(summary = "Create an invoice")
    public ResponseEntity<Map<String, Object>> create(@RequestBody InvoiceRequest req,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("invoice", invoiceService.create(req, user)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update invoice status")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                       @RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("invoice", invoiceService.updateStatus(id, body.get("status"), user)));
    }
}
