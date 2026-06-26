package com.finbridge.controller;

import com.finbridge.entity.FinancialProfile;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.FinancialProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/financial-profile")
@RequiredArgsConstructor
public class FinancialProfileController {
    private final FinancialProfileRepository profileRepository;

    @GetMapping
    public ResponseEntity<FinancialProfile> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found")));
    }

    @PostMapping
    public ResponseEntity<FinancialProfile> createOrUpdate(@RequestBody FinancialProfile profile,
                                                            @AuthenticationPrincipal User user) {
        return profileRepository.findByUserId(user.getId()).map(existing -> {
            existing.setAnnualIncome(profile.getAnnualIncome());
            existing.setMonthlyIncome(profile.getMonthlyIncome());
            existing.setMonthlyExpenses(profile.getMonthlyExpenses());
            existing.setSavings(profile.getSavings());
            existing.setEmergencyFund(profile.getEmergencyFund());
            existing.setCreditScore(profile.getCreditScore());
            existing.setRiskTolerance(profile.getRiskTolerance());
            existing.setInvestmentGoals(profile.getInvestmentGoals());
            existing.setBusinessName(profile.getBusinessName());
            existing.setBusinessType(profile.getBusinessType());
            existing.setAnnualRevenue(profile.getAnnualRevenue());
            existing.setAnnualExpenses(profile.getAnnualExpenses());
            existing.setYearsInBusiness(profile.getYearsInBusiness());
            return ResponseEntity.ok(profileRepository.save(existing));
        }).orElseGet(() -> {
            // Never trust a client-supplied id here: leaving it set would make save() overwrite
            // an existing (possibly another user's) row. Force an insert owned by this user.
            profile.setId(null);
            profile.setUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(profileRepository.save(profile));
        });
    }
}
