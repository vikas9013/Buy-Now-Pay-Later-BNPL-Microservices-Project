package com.bnpl.ledger.client;

import com.bnpl.ledger.dto.CreditAccountResponse;
import com.bnpl.ledger.dto.CreditDeductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient userServiceWebClient;

    public CreditAccountResponse getCreditAccount(UUID userId) {
        return userServiceWebClient.get()
                .uri("/api/users/{userId}/credit", userId)
                .retrieve()
                .bodyToMono(CreditAccountResponse.class)
                .block(); // Synchronous call for simple workflow
    }

    public CreditAccountResponse deductCredit(UUID userId, BigDecimal amount) {
        return userServiceWebClient.post()
                .uri("/api/users/{userId}/credit/deduct", userId)
                .bodyValue(new CreditDeductRequest(amount))
                .retrieve()
                .bodyToMono(CreditAccountResponse.class)
                .block();
    }

    public CreditAccountResponse restoreCredit(UUID userId, BigDecimal amount) {
        return userServiceWebClient.post()
                .uri("/api/users/{userId}/credit/restore", userId)
                .bodyValue(new CreditDeductRequest(amount))
                .retrieve()
                .bodyToMono(CreditAccountResponse.class)
                .block();
    }
}
