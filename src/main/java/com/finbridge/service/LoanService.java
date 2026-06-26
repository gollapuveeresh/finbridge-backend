package com.finbridge.service;

import com.finbridge.entity.Loan;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.LoanRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;

    public List<Loan> getByUser(UUID userId) { return loanRepository.findByUserIdAndActiveTrue(userId); }

    /** Load and authorize: only the owning user (or admin-tier staff) may access the record. */
    public Loan getById(UUID id, User actor) {
        Loan loan = load(id);
        OwnershipGuard.assertOwnerOrAdmin(actor, loan.getUser(), "loan");
        return loan;
    }

    @Transactional
    public Loan create(Loan loan) { return loanRepository.save(loan); }

    @Transactional
    public Loan update(UUID id, Loan patch, User actor) {
        Loan loan = getById(id, actor);
        if (patch.getStatus() != null) loan.setStatus(patch.getStatus());
        if (patch.getOutstandingBalance() != null) loan.setOutstandingBalance(patch.getOutstandingBalance());
        if (patch.getNotes() != null) loan.setNotes(patch.getNotes());
        return loanRepository.save(loan);
    }

    @Transactional
    public void softDelete(UUID id, User actor) {
        Loan loan = getById(id, actor);
        loan.setActive(false);
        loanRepository.save(loan);
    }

    private Loan load(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + id));
    }
}
