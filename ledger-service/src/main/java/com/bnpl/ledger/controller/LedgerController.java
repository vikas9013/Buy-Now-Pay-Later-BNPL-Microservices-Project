package com.bnpl.ledger.controller;

import com.bnpl.ledger.dto.CheckoutRequest;
import com.bnpl.ledger.dto.CheckoutResponse;
import com.bnpl.ledger.dto.PaymentOrderRequest;
import com.bnpl.ledger.dto.PaymentOrderResponse;
import com.bnpl.ledger.dto.RepaymentRequest;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
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

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {

        // Check if Idempotency Key was already processed
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            CheckoutResponse cachedResponse = idempotencyService.getResponse(idempotencyKey, CheckoutResponse.class);
            if (cachedResponse != null) {
                return ResponseEntity.status(HttpStatus.OK).body(cachedResponse);
            }
        }

        CheckoutResponse response = ledgerService.processCheckout(request);

        // Cache response in Redis with Idempotency Key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveResponse(idempotencyKey, response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        }

        Repayment response = ledgerService.processRepayment(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveResponse(idempotencyKey, response);
        }

        return ResponseEntity.ok(response);
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

    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getUserTransactions(userId));
    }

    @GetMapping("/repayments/pending/{userId}")
    public ResponseEntity<List<Repayment>> getUserPendingRepayments(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getUserPendingRepayments(userId));
    }
}
