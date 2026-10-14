package com.bnpl.ledger.controller;

import com.bnpl.ledger.dto.CheckoutRequest;
import com.bnpl.ledger.dto.CheckoutResponse;
import com.bnpl.ledger.dto.FinancialAdvisorResponse;
import com.bnpl.ledger.dto.PaymentOrderRequest;
import com.bnpl.ledger.dto.PaymentOrderResponse;
import com.bnpl.ledger.dto.RepaymentRequest;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
import com.bnpl.ledger.service.AiFinancialAdvisorService;
import com.bnpl.ledger.service.IdempotencyService;
import com.bnpl.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final AiFinancialAdvisorService aiFinancialAdvisorService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {

        // 1. Check if Idempotency Key was already processed
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            CheckoutResponse cachedResponse = idempotencyService.getResponse(idempotencyKey, CheckoutResponse.class);
            if (cachedResponse != null) {
                return ResponseEntity.status(HttpStatus.OK).body(cachedResponse);
            }

            // 2. Atomically acquire lock to prevent simultaneous duplicate execution
            boolean lockAcquired = idempotencyService.tryAcquireLock(idempotencyKey);
            if (!lockAcquired) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        try {
            CheckoutResponse response = ledgerService.processCheckout(request);

            // 3. Cache response in Redis with Idempotency Key
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.saveResponse(idempotencyKey, response);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.releaseLock(idempotencyKey);
            }
            throw ex;
        }
    }

    @PostMapping("/repay")
    public ResponseEntity<Repayment> repay(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RepaymentRequest request) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Repayment cachedResponse = idempotencyService.getResponse(idempotencyKey, Repayment.class);
            if (cachedResponse != null) {
                return ResponseEntity.status(HttpStatus.OK).body(cachedResponse);
            }

            boolean lockAcquired = idempotencyService.tryAcquireLock(idempotencyKey);
            if (!lockAcquired) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        try {
            Repayment response = ledgerService.processRepayment(request);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.saveResponse(idempotencyKey, response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.releaseLock(idempotencyKey);
            }
            throw ex;
        }
    }

    @PostMapping({"/payment/create-order", "/payments/create-order"})
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@Valid @RequestBody PaymentOrderRequest request) {
        return ResponseEntity.ok(ledgerService.createPaymentOrder(request));
    }

    @PostMapping({"/payment/verify-and-repay", "/payments/verify-and-repay"})
    public ResponseEntity<Repayment> verifyAndRepay(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RepaymentRequest request) {
        return repay(idempotencyKey, request);
    }

    @PostMapping("/payment/webhook/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload) {
        return ResponseEntity.ok(ledgerService.handleRazorpayWebhook(payload, signature));
    }

    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getUserTransactions(userId));
    }

    @GetMapping("/repayments/pending/{userId}")
    public ResponseEntity<List<Repayment>> getUserPendingRepayments(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getUserPendingRepayments(userId));
    }

    @GetMapping("/ai/financial-insights/{userId}")
    public ResponseEntity<FinancialAdvisorResponse> getFinancialAdvisorInsights(@PathVariable UUID userId) {
        return ResponseEntity.ok(aiFinancialAdvisorService.generateFinancialInsights(userId));
    }
}
