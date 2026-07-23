package com.bnpl.ledger.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CheckoutRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 12, message = "Maximum EMI tenure is 12 months")
    private Integer tenureMonths = 1; // Default to 1 month (Pay in 30 days @ 0% interest)
}
