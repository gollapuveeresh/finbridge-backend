package com.finbridge.controller;

import com.finbridge.dto.*;
import com.finbridge.entity.User;
import com.finbridge.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals", description = "Proposal lifecycle management")
public class ProposalController {

    private final ProposalService proposalService;

    @GetMapping
    @Operation(summary = "Get proposals for current user, optionally by department")
    public ResponseEntity<Map<String, Object>> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(Map.of("proposals", proposalService.getForUser(user, department)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get proposal by ID")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("proposal", proposalService.getResponse(id)));
    }

    @PostMapping
    @Operation(summary = "Create proposal")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody ProposalRequest request,
                                                    @AuthenticationPrincipal User user) {
        log.info("Creating proposal: {} by consultant {}", request.title(), user.getId());
        return ResponseEntity.ok(Map.of("proposal", proposalService.create(request, user)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update proposal (fields or status + feedback)")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                    @RequestBody Map<String, Object> body) {
        log.info("Updating proposal: {}", id);
        ProposalResponse updated;
        if (body.containsKey("status")) {
            updated = proposalService.updateStatus(id, (String) body.get("status"), (String) body.get("clientFeedback"));
        } else {
            updated = proposalService.update(id, new ProposalRequest(
                    null, null, null, null,
                    (String) body.get("title"), (String) body.get("summary"), null, null));
        }
        return ResponseEntity.ok(Map.of("proposal", updated));
    }

    @PatchMapping("/{id}/decision")
    @Operation(summary = "Client approves/rejects/requests changes on proposal")
    public ResponseEntity<Map<String, Object>> decision(@PathVariable UUID id,
                                                      @Valid @RequestBody ProposalDecisionRequest request) {
        log.info("Proposal decision: {} -> {}", id, request.status());
        return ResponseEntity.ok(Map.of("proposal",
                proposalService.updateStatus(id, request.status(), request.feedback())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (deactivate) a proposal")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        proposalService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Proposal deleted"));
    }
}
