package com.bnpl.ledger.dto;

import com.bnpl.ledger.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CheckoutResponse {
    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String merchantName;
    private LocalDateTime transactionDate;
    private TransactionStatus status;
    private LocalDate repaymentDueDate;
}
