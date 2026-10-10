package com.bnpl.ledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderRequest {

    @NotNull(message = "Repayment ID is required")
    private UUID repaymentId;

    @Builder.Default
    private String gateway = "RAZORPAY"; // RAZORPAY or STRIPE

    private String customerEmail;
    private String customerPhone;
}
