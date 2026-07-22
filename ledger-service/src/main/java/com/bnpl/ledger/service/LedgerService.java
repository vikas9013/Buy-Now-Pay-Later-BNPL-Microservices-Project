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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final TransactionRepository transactionRepository;
    private final RepaymentRepository repaymentRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public CheckoutResponse processCheckout(CheckoutRequest request) {
        // 1. Call User Service via REST to check credit limit
        CreditAccountResponse creditAccount = userServiceClient.getCreditAccount(request.getUserId());

        if (!"ACTIVE".equalsIgnoreCase(creditAccount.getAccountStatus())) {
            throw new IllegalStateException("User account is not ACTIVE. Current status: " + creditAccount.getAccountStatus());
        }

        if (creditAccount.getAvailableCredit().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient credit limit! Available: ₹" + creditAccount.getAvailableCredit());
        }

        // 2. Call User Service via REST to deduct credit limit
        userServiceClient.deductCredit(request.getUserId(), request.getAmount());

        // 3. Save Transaction
        Transaction transaction = Transaction.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .merchantName(request.getMerchantName())
                .transactionDate(LocalDateTime.now())
                .status(TransactionStatus.SUCCESS)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 4. Create Repayment Schedule (Due in 30 days)
        LocalDate dueDate = LocalDate.now().plusDays(30);
        Repayment repayment = Repayment.builder()
                .userId(request.getUserId())
                .transaction(savedTransaction)
                .amountDue(request.getAmount())
                .dueDate(dueDate)
                .isPaid(false)
                .lateFeeApplied(BigDecimal.ZERO)
                .build();

        repaymentRepository.save(repayment);

        // 5. Publish Kafka Event asynchronously for consumers (e.g. Notification Service)
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
                .amount(savedTransaction.getAmount())
                .merchantName(savedTransaction.getMerchantName())
                .transactionDate(savedTransaction.getTransactionDate())
                .status(savedTransaction.getStatus())
                .repaymentDueDate(dueDate)
                .build();
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

        // Mark repayment as paid
        repayment.setPaid(true);
        Repayment savedRepayment = repaymentRepository.save(repayment);

        // Call User Service via REST to restore credit limit
        userServiceClient.restoreCredit(repayment.getUserId(), repayment.getAmountDue());

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
