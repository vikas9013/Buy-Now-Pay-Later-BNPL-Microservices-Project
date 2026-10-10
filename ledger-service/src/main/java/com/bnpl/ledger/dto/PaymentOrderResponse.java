package com.bnpl.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {

    private String orderId;
    private UUID repaymentId;
    private BigDecimal amount;
    private Long amountInSubunits; // e.g., paise (INR) or cents (USD)
    private String currency;
    private String gateway; // RAZORPAY or STRIPE
    private String keyId; // Public API Key for client checkout
    private String status; // CREATED
    private String simulatedSignature; // Provided in sandbox/test mode for automated testing
}
