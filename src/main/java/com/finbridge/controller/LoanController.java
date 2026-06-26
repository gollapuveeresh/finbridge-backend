package com.finbridge.controller;

import com.finbridge.entity.Loan;
import com.finbridge.entity.User;
import com.finbridge.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @GetMapping
    public ResponseEntity<List<Loan>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.getByUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getOne(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.getById(id, user));
    }

    @PostMapping
    public ResponseEntity<Loan> create(@RequestBody Loan loan, @AuthenticationPrincipal User user) {
        loan.setUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(loan));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Loan> update(@PathVariable UUID id, @RequestBody Loan patch,
                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(loanService.update(id, patch, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        loanService.softDelete(id, user);
        return ResponseEntity.noContent().build();
    }
}
