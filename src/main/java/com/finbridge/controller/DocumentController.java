package com.finbridge.controller;

import com.finbridge.entity.User;
import com.finbridge.security.SecurityRoles;
import com.finbridge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Client document aggregation for staff")
@PreAuthorize(SecurityRoles.STAFF)
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/consultant")
    @Operation(summary = "Documents across the consultant's own cases")
    public ResponseEntity<Map<String, Object>> consultant(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("documents", documentService.forConsultant(user)));
    }

    @GetMapping("/department")
    @Operation(summary = "Documents across the department's cases")
    public ResponseEntity<Map<String, Object>> department(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("documents", documentService.forDepartment(user)));
    }
}
