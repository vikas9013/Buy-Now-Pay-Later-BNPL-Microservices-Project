package com.bnpl.ledger.dto;

import com.bnpl.ledger.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    private UUID transactionId;
    private UUID userId;
    private BigDecimal principalAmount;
    private BigDecimal interestRatePercent;
    private BigDecimal totalAmountWithInterest;
    private Integer tenureMonths;
    private BigDecimal monthlyInstallment;
    private String merchantName;
    private LocalDateTime transactionDate;
    private TransactionStatus status;
}
