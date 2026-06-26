package com.finbridge.controller;

import com.finbridge.dto.ConsultationResponse;
import com.finbridge.entity.Consultation;
import com.finbridge.entity.User;
import com.finbridge.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {
    private final ConsultationService consultationService;

    @GetMapping
    public ResponseEntity<List<ConsultationResponse>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.getForUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponse> getOne(@PathVariable UUID id,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.getOne(id, user));
    }

    @PostMapping
    public ResponseEntity<ConsultationResponse> create(@RequestBody Consultation consultation,
                                                @AuthenticationPrincipal User user) {
        if (consultation.getClient() == null || (!"consultant".equalsIgnoreCase(user.getRole()) &&
                !"department-admin".equalsIgnoreCase(user.getRole()) &&
                !"admin".equalsIgnoreCase(user.getRole()) &&
                !"crm-admin".equalsIgnoreCase(user.getRole()) &&
                !"super-admin".equalsIgnoreCase(user.getRole()))) {
            consultation.setClient(user);
        }
        if (consultation.getConsultant() == null && "consultant".equalsIgnoreCase(user.getRole())) {
            consultation.setConsultant(user);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(consultationService.create(consultation));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ConsultationResponse> update(@PathVariable UUID id, @RequestBody Consultation patch,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.update(id, patch, user));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<Map<String, Object>> accept(@PathVariable UUID id,
                                                       @RequestBody Map<String, Object> body,
                                                       @AuthenticationPrincipal User user) {
        String confirmedDate = body.get("confirmedDate") != null ? body.get("confirmedDate").toString() : null;
        String confirmedTime = body.get("confirmedTime") != null ? body.get("confirmedTime").toString() : null;
        Boolean recordingEnabled = body.containsKey("recordingEnabled") && body.get("recordingEnabled") != null 
                ? Boolean.valueOf(body.get("recordingEnabled").toString()) : false;
        ConsultationResponse c = consultationService.accept(id, confirmedDate, confirmedTime, recordingEnabled, user);
        return ResponseEntity.ok(Map.of("status", "success", "consultation", c));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<Map<String, Object>> assign(@PathVariable UUID id,
                                                      @RequestBody Map<String, String> body,
                                                      @AuthenticationPrincipal User user) {
        ConsultationResponse c = consultationService.assign(id, parseConsultantId(body.get("consultantId")), user);
        return ResponseEntity.ok(Map.of("consultation", c));
    }

    @PatchMapping("/{id}/schedule")
    public ResponseEntity<ConsultationResponse> schedule(@PathVariable UUID id,
                                                         @RequestBody Map<String, Object> body,
                                                         @AuthenticationPrincipal User user) {
        String confirmedDate = body.get("confirmedDate") != null ? body.get("confirmedDate").toString() : null;
        String confirmedTime = body.get("confirmedTime") != null ? body.get("confirmedTime").toString() : null;
        String meetingLink = body.get("meetingLink") != null ? body.get("meetingLink").toString() : null;
        Boolean recordingEnabled = body.containsKey("recordingEnabled") && body.get("recordingEnabled") != null 
                ? Boolean.valueOf(body.get("recordingEnabled").toString()) : false;
        return ResponseEntity.ok(consultationService.schedule(id, confirmedDate, confirmedTime, meetingLink, recordingEnabled, user));
    }

    @PatchMapping("/{id}/send-to-client")
    public ResponseEntity<ConsultationResponse> sendToClient(@PathVariable UUID id,
                                                             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.sendToClient(id, user));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ConsultationResponse> complete(@PathVariable UUID id,
                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.complete(id, user));
    }

    @PostMapping("/{id}/verify-complete")
    public ResponseEntity<ConsultationResponse> verifyComplete(@PathVariable UUID id,
                                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.verifyComplete(id, user));
    }

    @GetMapping("/completed-list")
    public ResponseEntity<List<ConsultationResponse>> completedList(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.getCompletedByConsultantForAdmin(user));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<Map<String, Object>>> payments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.getPaymentsForConsultant(user));
    }

    @GetMapping("/payments/admin")
    public ResponseEntity<List<Map<String, Object>>> paymentsForAdmin(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(consultationService.getPaymentsForAdmin(user));
    }

    /** Parse the consultantId, turning malformed input into a clean 400 instead of a 500. */
    private static UUID parseConsultantId(String raw) {
        if (raw == null || raw.isBlank())
            throw new com.finbridge.exception.BadRequestException("consultantId is required");
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new com.finbridge.exception.BadRequestException("consultantId is not a valid id");
        }
    }
}
