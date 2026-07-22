package com.bnpl.ledger.event;

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
public class CheckoutEvent {
    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String merchantName;
    private LocalDateTime timestamp;
}
