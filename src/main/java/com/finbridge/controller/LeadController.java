package com.finbridge.controller;

import com.finbridge.dto.*;
import com.finbridge.entity.Lead;
import com.finbridge.entity.User;
import com.finbridge.security.SecurityRoles;
import com.finbridge.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "CRM Lead management")
@PreAuthorize(SecurityRoles.STAFF)   // CRM leads are internal-only; clients have no access
public class LeadController {

    private final LeadService leadService;
    private final DtoMapper mapper;

    @PostMapping("/capture")
    @PreAuthorize("permitAll()")      // public website form
    @Operation(summary = "Public lead capture from website form")
    public ResponseEntity<LeadResponse> capture(@Valid @RequestBody LeadRequest request) {
        log.info("Lead captured from website: {}", request.email());
        Lead lead = leadService.create(mapper.toLead(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toLeadResponse(lead));
    }

    @GetMapping
    @Operation(summary = "Get all leads with optional department/status filter")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User user) {
        String finalDepartment = department;
        if (user != null && ("consultant".equalsIgnoreCase(user.getRole()) || "department-admin".equalsIgnoreCase(user.getRole()))) {
            if (user.getDepartment() != null && !user.getDepartment().isBlank()) {
                finalDepartment = user.getDepartment();
            }
        }
        List<LeadResponse> leads = leadService.getFiltered(finalDepartment, status)
                .stream().map(mapper::toLeadResponse).toList();
        return ResponseEntity.ok(Map.of("leads", leads));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get lead pipeline stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(leadService.getStats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lead by ID")
    public ResponseEntity<LeadResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toLeadResponse(leadService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new lead")
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        log.info("Creating lead: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toLeadResponse(leadService.create(mapper.toLead(request))));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a lead (status, department, score, etc.)")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                @RequestBody LeadUpdateRequest request) {
        log.info("Updating lead: {}", id);
        return ResponseEntity.ok(Map.of("lead",
                mapper.toLeadResponse(leadService.update(id, request))));
    }

    @PostMapping("/{id}/note")
    @Operation(summary = "Add note to lead")
    public ResponseEntity<Map<String, Object>> addNote(@PathVariable UUID id,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal User user) {
        String actor = user != null ? user.getName() : "CRM";
        return ResponseEntity.ok(Map.of("lead",
                mapper.toLeadResponse(leadService.addNote(id, body.get("text"), actor))));
    }

    @PostMapping("/{id}/send-to-department")
    @Operation(summary = "Route a lead to a department")
    public ResponseEntity<Map<String, Object>> sendToDepartment(@PathVariable UUID id,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal User user) {
        String actor = user != null ? user.getName() : "CRM";
        Lead lead = leadService.sendToDepartment(id, body.get("department"), body.get("notes"), actor);
        return ResponseEntity.ok(Map.of("lead", mapper.toLeadResponse(lead)));
    }

    @PostMapping("/{id}/send-fee-proposal")
    @Operation(summary = "Send a consultation fee proposal to the client")
    public ResponseEntity<Map<String, Object>> sendFeeProposal(@PathVariable UUID id,
                                                               @AuthenticationPrincipal User user) {
        log.info("Sending fee proposal for lead {} by admin {}", id, user != null ? user.getEmail() : "anonymous");
        Lead lead = leadService.sendFeeProposal(id, user);
        return ResponseEntity.ok(Map.of("lead", mapper.toLeadResponse(lead)));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Convert lead to client")
    public ResponseEntity<Map<String, Object>> convert(@PathVariable UUID id) {
        log.info("Converting lead to client: {}", id);
        LeadService.ConversionResult result = leadService.convertToClient(id);
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("isNewClient", result.isNewClient());
        resp.put("tempPassword", result.tempPassword());
        resp.put("client", mapper.toStaffResponse(result.client()));
        return ResponseEntity.ok(resp);
    }
}
