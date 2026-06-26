package com.finbridge.service;

import com.finbridge.entity.Investment;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.InvestmentRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvestmentService {
    private final InvestmentRepository investmentRepository;

    public List<Investment> getByUser(UUID userId) {
        return investmentRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Load and authorize: only the owning user (or admin-tier staff) may access the
     * record.
     */
    public Investment getById(UUID id, User actor) {
        Investment inv = load(id);
        OwnershipGuard.assertOwnerOrAdmin(actor, inv.getUser(), "investment");
        return inv;
    }

    @Transactional
    public Investment create(Investment inv) {
        return investmentRepository.save(inv);
    }

    @Transactional
    public Investment update(UUID id, Investment patch, User actor) {
        Investment inv = getById(id, actor);
        if (patch.getCurrentValue() != null)
            inv.setCurrentValue(patch.getCurrentValue());
        if (patch.getNotes() != null)
            inv.setNotes(patch.getNotes());
        if (patch.getRiskLevel() != null)
            inv.setRiskLevel(patch.getRiskLevel());
        return investmentRepository.save(inv);
    }

    @Transactional
    public void softDelete(UUID id, User actor) {
        Investment inv = getById(id, actor);
        inv.setActive(false);
        investmentRepository.save(inv);
    }

    private Investment load(UUID id) {
        return investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found: " + id));
    }
}
