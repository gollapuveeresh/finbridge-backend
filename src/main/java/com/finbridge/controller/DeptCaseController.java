package com.finbridge.controller;

import com.finbridge.entity.User;
import com.finbridge.security.SecurityRoles;
import com.finbridge.service.DeptCaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dept-cases")
@RequiredArgsConstructor
@Tag(name = "Department Cases", description = "Tax / investment / insurance / wealth workflow")
@PreAuthorize(SecurityRoles.STAFF)
public class DeptCaseController {

    private final DeptCaseService deptCaseService;

    @GetMapping("/{dept}")
    public ResponseEntity<Map<String, Object>> list(@PathVariable String dept) {
        return ResponseEntity.ok(Map.of("cases", deptCaseService.getByDepartment(dept)));
    }

    @PostMapping("/{dept}")
    public ResponseEntity<Map<String, Object>> create(@PathVariable String dept,
                                                      @RequestBody Map<String, Object> body,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("case", deptCaseService.create(dept, body, user)));
    }

    @PatchMapping("/{dept}/{caseId}")
    public ResponseEntity<Map<String, Object>> patch(@PathVariable String dept, @PathVariable UUID caseId,
                                                     @RequestBody Map<String, Object> body,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("case", deptCaseService.patch(caseId, body, user)));
    }

    @PatchMapping("/{dept}/{caseId}/document/{docId}")
    public ResponseEntity<Map<String, Object>> updateDocument(@PathVariable String dept, @PathVariable UUID caseId,
                                                              @PathVariable String docId,
                                                              @RequestBody Map<String, String> body,
                                                              @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("case",
                deptCaseService.updateDocument(caseId, docId, body.get("status"), body.get("rejectionNote"), user)));
    }

    @PostMapping("/{dept}/{caseId}/note")
    public ResponseEntity<Map<String, Object>> addNote(@PathVariable String dept, @PathVariable UUID caseId,
                                                       @RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal User user) {
        String actor = user != null ? user.getName() : "Consultant";
        return ResponseEntity.ok(Map.of("case", deptCaseService.addNote(caseId, body.get("text"), actor, user)));
    }
}
