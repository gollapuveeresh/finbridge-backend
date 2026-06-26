package com.finbridge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public liveness endpoint at /api/health (permitted in SecurityConfig and listed as a
 * public endpoint by the frontend). Previously the only health mapping lived under
 * /api/dashboard/health, so /api/health had no handler and returned 500.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "success", "message", "FinBridge API is healthy"));
    }
}
