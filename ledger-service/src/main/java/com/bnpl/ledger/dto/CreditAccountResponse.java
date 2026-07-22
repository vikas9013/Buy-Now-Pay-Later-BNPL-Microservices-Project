package com.bnpl.ledger.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreditAccountResponse {
    private UUID accountId;
    private UUID userId;
    private BigDecimal totalCreditLimit;
    private BigDecimal availableCredit;
    private String accountStatus;
}
