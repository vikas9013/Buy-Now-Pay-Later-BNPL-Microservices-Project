package com.bnpl.ledger.repository;

import com.bnpl.ledger.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {
    List<Repayment> findByIsPaidFalseAndDueDateBefore(LocalDate date);
    List<Repayment> findByTransactionId(UUID transactionId);
    List<Repayment> findByUserIdAndIsPaidFalse(UUID userId);
    List<Repayment> findByUserId(UUID userId);
}
