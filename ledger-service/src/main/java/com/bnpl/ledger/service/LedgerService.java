package com.bnpl.ledger.service;

import com.bnpl.ledger.client.UserServiceClient;
import com.bnpl.ledger.dto.CheckoutRequest;
import com.bnpl.ledger.dto.CheckoutResponse;
import com.bnpl.ledger.dto.CreditAccountResponse;
import com.bnpl.ledger.dto.RepaymentRequest;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
import com.bnpl.ledger.enums.TransactionStatus;
import com.bnpl.ledger.event.CheckoutEvent;
import com.bnpl.ledger.event.RepaymentEvent;
import com.bnpl.ledger.repository.RepaymentRepository;
import com.bnpl.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final RepaymentRepository repaymentRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaProducerService kafkaProducerService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RedissonClient redissonClient;

    @Transactional
    public CheckoutResponse processCheckout(CheckoutRequest request) {
        String lockKey = "lock:checkout:user:" + request.getUserId();
        RLock lock = (redissonClient != null) ? redissonClient.getLock(lockKey) : null;

        try {
            if (lock != null) {
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new IllegalStateException("Could not acquire lock for user checkout. Concurrent request in progress.");
                }
                log.info("🔒 Redisson Lock acquired for checkout: {}", lockKey);
            }

            int tenure = (request.getTenureMonths() != null && request.getTenureMonths() > 0) 
                    ? request.getTenureMonths() : 1;

            // 1. Calculate Interest Rate according to tenure (Up to 12 months)
            BigDecimal interestRatePercent = getInterestRateForTenure(tenure);
            BigDecimal interestAmount = request.getAmount()
                    .multiply(interestRatePercent)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal totalAmountWithInterest = request.getAmount().add(interestAmount);
            BigDecimal monthlyInstallment = totalAmountWithInterest
                    .divide(new BigDecimal(tenure), 2, RoundingMode.HALF_UP);

            // 2. Call User Service via REST to check credit limit against total amount
            CreditAccountResponse creditAccount = userServiceClient.getCreditAccount(request.getUserId());

            if (!"ACTIVE".equalsIgnoreCase(creditAccount.getAccountStatus())) {
                throw new IllegalStateException("User account is not ACTIVE. Current status: " + creditAccount.getAccountStatus());
            }

            if (creditAccount.getAvailableCredit().compareTo(request.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient credit limit! Available: ₹" + creditAccount.getAvailableCredit());
            }

            // 3. Call User Service via REST to deduct principal amount
            userServiceClient.deductCredit(request.getUserId(), request.getAmount());

            // 4. Save Transaction Record
            Transaction transaction = Transaction.builder()
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .merchantName(request.getMerchantName() + " (" + tenure + "M EMI @ " + interestRatePercent + "% Int)")
                    .transactionDate(LocalDateTime.now())
                    .status(TransactionStatus.SUCCESS)
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            // 5. Generate EMI Repayment Schedule for 1 to 12 months
            for (int i = 1; i <= tenure; i++) {
                LocalDate dueDate = LocalDate.now().plusMonths(i);

                Repayment repayment = Repayment.builder()
                        .userId(request.getUserId())
                        .transaction(savedTransaction)
                        .amountDue(monthlyInstallment)
                        .dueDate(dueDate)
                        .isPaid(false)
                        .lateFeeApplied(BigDecimal.ZERO)
                        .build();

                repaymentRepository.save(repayment);
            }

            // 6. Publish Kafka Event asynchronously
            try {
                CheckoutEvent event = CheckoutEvent.builder()
                        .transactionId(savedTransaction.getId())
                        .userId(savedTransaction.getUserId())
                        .amount(savedTransaction.getAmount())
                        .merchantName(savedTransaction.getMerchantName())
                        .timestamp(savedTransaction.getTransactionDate())
                        .build();
                kafkaProducerService.sendCheckoutEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish CheckoutEvent to Kafka: {}", e.getMessage());
            }

            return CheckoutResponse.builder()
                    .transactionId(savedTransaction.getId())
                    .userId(savedTransaction.getUserId())
                    .principalAmount(request.getAmount())
                    .interestRatePercent(interestRatePercent)
                    .totalAmountWithInterest(totalAmountWithInterest)
                    .tenureMonths(tenure)
                    .monthlyInstallment(monthlyInstallment)
                    .merchantName(savedTransaction.getMerchantName())
                    .transactionDate(savedTransaction.getTransactionDate())
                    .status(savedTransaction.getStatus())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Checkout interrupted", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 Redisson Lock released: {}", lockKey);
            }
        }
    }

    private BigDecimal getInterestRateForTenure(int tenureMonths) {
        return switch (tenureMonths) {
            case 1 -> BigDecimal.ZERO;                     // Pay in 30 Days (0% Interest)
            case 2, 3 -> new BigDecimal("5.00");           // 3 Months EMI (5% Total Interest)
            case 4, 5, 6 -> new BigDecimal("10.00");       // 6 Months EMI (10% Total Interest)
            case 7, 8, 9 -> new BigDecimal("12.00");       // 9 Months EMI (12% Total Interest)
            case 10, 11, 12 -> new BigDecimal("15.00");    // 12 Months EMI (15% Total Interest)
            default -> new BigDecimal("15.00");
        };
    }

    @Transactional
    public Repayment processRepayment(RepaymentRequest request) {
        Repayment repayment = repaymentRepository.findById(request.getRepaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found for ID: " + request.getRepaymentId()));

        if (repayment.isPaid()) {
            throw new IllegalStateException("Repayment is already completed!");
        }

        BigDecimal totalDue = repayment.getAmountDue().add(repayment.getLateFeeApplied());
        if (request.getAmount().compareTo(totalDue) < 0) {
            throw new IllegalArgumentException("Payment amount ₹" + request.getAmount() + " is less than total due ₹" + totalDue);
        }

        // Mark repayment installment as paid
        repayment.setPaid(true);
        Repayment savedRepayment = repaymentRepository.save(repayment);

        // Calculate principal share per installment to restore user credit
        int totalTenure = repaymentRepository.findByTransactionId(repayment.getTransaction().getId()).size();
        BigDecimal principalToRestore = repayment.getTransaction().getAmount()
                .divide(new BigDecimal(totalTenure), 2, RoundingMode.HALF_UP);

        // Call User Service via REST to restore credit limit
        userServiceClient.restoreCredit(repayment.getUserId(), principalToRestore);

        // Publish Kafka Repayment Event
        try {
            RepaymentEvent event = RepaymentEvent.builder()
                    .repaymentId(savedRepayment.getId())
                    .userId(savedRepayment.getUserId())
                    .amount(savedRepayment.getAmountDue())
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaProducerService.sendRepaymentEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish RepaymentEvent to Kafka: {}", e.getMessage());
        }

        return savedRepayment;
    }

    public List<Transaction> getUserTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    public List<Repayment> getUserPendingRepayments(UUID userId) {
        return repaymentRepository.findByUserIdAndIsPaidFalse(userId);
    }
}
