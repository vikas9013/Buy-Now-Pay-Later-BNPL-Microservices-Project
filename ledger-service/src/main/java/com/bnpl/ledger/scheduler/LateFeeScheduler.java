package com.bnpl.ledger.scheduler;

import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LateFeeScheduler {

    private final RepaymentRepository repaymentRepository;

    private static final BigDecimal LATE_FEE_AMOUNT = new BigDecimal("500.00");

    // Runs every day at midnight (or every minute for demo testing if modified)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processOverdueRepayments() {
        log.info("Running Late Fee Scheduler job...");

        LocalDate today = LocalDate.now();
        List<Repayment> overdueRepayments = repaymentRepository.findByIsPaidFalseAndDueDateBefore(today);

        for (Repayment repayment : overdueRepayments) {
            if (repayment.getLateFeeApplied().compareTo(BigDecimal.ZERO) == 0) {
                repayment.setLateFeeApplied(LATE_FEE_AMOUNT);
                repaymentRepository.save(repayment);
                log.info("Applied late fee of ₹{} to repayment ID: {} for user: {}", 
                        LATE_FEE_AMOUNT, repayment.getId(), repayment.getUserId());
            }
        }
    }
}
