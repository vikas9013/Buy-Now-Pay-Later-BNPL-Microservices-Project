package com.bnpl.ledger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RepaymentRequest {

    @NotNull(message = "Repayment ID is required")
    private UUID repaymentId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String paymentGateway;     // RAZORPAY, STRIPE, INTERNAL
    private String gatewayOrderId;     // e.g. order_xxx
    private String gatewayPaymentId;   // e.g. pay_xxx
    private String gatewaySignature;   // HMAC SHA-256 signature
    private String paymentMethod;      // UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING
}
