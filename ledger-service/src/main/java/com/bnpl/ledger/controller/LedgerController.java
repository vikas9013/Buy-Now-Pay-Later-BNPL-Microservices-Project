package com.bnpl.ledger.controller;

import com.bnpl.ledger.dto.CheckoutRequest;
import com.bnpl.ledger.dto.CheckoutResponse;
import com.bnpl.ledger.dto.RepaymentRequest;
import com.bnpl.ledger.entity.Repayment;
import com.bnpl.ledger.entity.Transaction;
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

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = ledgerService.processCheckout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/repay")
    public ResponseEntity<Repayment> repay(@Valid @RequestBody RepaymentRequest request) {
        Repayment repayment = ledgerService.processRepayment(request);
        return ResponseEntity.ok(repayment);
    }

    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getUserTransactions(userId));
    }

    @GetMapping("/repayments/pending/{userId}")
    public ResponseEntity<List<Repayment>> getPendingRepayments(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getUserPendingRepayments(userId));
    }
}
