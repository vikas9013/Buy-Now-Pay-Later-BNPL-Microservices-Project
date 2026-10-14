package com.bnpl.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditUpgradeRequest {

    @NotNull(message = "New credit limit is required")
    @DecimalMin(value = "1000.00", message = "New credit limit must be at least ₹1,000.00")
    @DecimalMax(value = "200000.00", message = "Maximum allowed credit limit is ₹2,00,000.00")
    private BigDecimal newCreditLimit;

    private String reason;
}
