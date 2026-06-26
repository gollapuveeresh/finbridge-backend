package com.finbridge.controller;

import com.finbridge.entity.User;
import com.finbridge.security.SecurityRoles;
import com.finbridge.service.LoanCaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan-cases")
@RequiredArgsConstructor
@Tag(name = "Loan Cases", description = "Loan case workflow (eligibility → disbursement → EMI)")
@PreAuthorize(SecurityRoles.STAFF)
public class LoanCaseController {

    private final LoanCaseService loanCaseService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("cases", loanCaseService.getForUser(user)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("loanCase", loanCaseService.create(body, user)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> patch(@PathVariable UUID id,
                                                     @RequestBody Map<String, Object> body,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("loanCase", loanCaseService.patch(id, body, user)));
    }

    @PatchMapping("/{id}/document/{docId}")
    public ResponseEntity<Map<String, Object>> updateDocument(@PathVariable UUID id, @PathVariable UUID docId,
                                                              @RequestBody Map<String, String> body,
                                                              @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("loanCase",
                loanCaseService.updateDocument(id, docId, body.get("status"), body.get("rejectionNote"), user)));
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<Map<String, Object>> disburse(@PathVariable UUID id,
                                                        @RequestBody Map<String, Object> body,
                                                        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("loanCase", loanCaseService.disburse(id, body, user)));
    }

    @PatchMapping("/{id}/emi/{emiId}")
    public ResponseEntity<Map<String, Object>> updateEmi(@PathVariable UUID id, @PathVariable UUID emiId,
                                                         @RequestBody Map<String, Object> body,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("loanCase",
                loanCaseService.updateEmi(id, emiId, (String) body.get("status"), body.get("paidDate"), user)));
    }

    @PostMapping("/{id}/note")
    public ResponseEntity<Map<String, Object>> addNote(@PathVariable UUID id,
                                                       @RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal User user) {
        String actor = user != null ? user.getName() : "Consultant";
        return ResponseEntity.ok(Map.of("loanCase", loanCaseService.addNote(id, body.get("text"), actor, user)));
    }
}
