package com.bnpl.user.dto;

import com.bnpl.user.enums.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CreditAccountResponse {
    private UUID accountId;
    private UUID userId;
    private BigDecimal totalCreditLimit;
    private BigDecimal availableCredit;
    private AccountStatus accountStatus;
}
