package com.finbridge.repository;

import com.finbridge.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    List<Loan> findByUserIdAndActiveTrue(UUID userId);
}
