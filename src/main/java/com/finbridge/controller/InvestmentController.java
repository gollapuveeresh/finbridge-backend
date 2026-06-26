package com.finbridge.controller;

import com.finbridge.entity.Investment;
import com.finbridge.entity.User;
import com.finbridge.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {
    private final InvestmentService investmentService;

    @GetMapping
    public ResponseEntity<List<Investment>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(investmentService.getByUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Investment> getOne(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(investmentService.getById(id, user));
    }

    @PostMapping
    public ResponseEntity<Investment> create(@RequestBody Investment inv, @AuthenticationPrincipal User user) {
        inv.setUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.create(inv));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Investment> update(@PathVariable UUID id, @RequestBody Investment patch,
                                             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(investmentService.update(id, patch, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        investmentService.softDelete(id, user);
        return ResponseEntity.noContent().build();
    }
}
