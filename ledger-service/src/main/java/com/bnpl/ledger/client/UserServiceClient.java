package com.bnpl.ledger.client;

import com.bnpl.ledger.dto.CreditAccountResponse;
import com.bnpl.ledger.dto.CreditDeductRequest;
import lombok.RequiredArgsConstructor;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient userServiceWebClient;

    @org.springframework.beans.factory.annotation.Value("${internal.service.secret:bnpl-internal-system-secret-2026}")
    private String internalServiceSecret;

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public CreditAccountResponse getCreditAccount(UUID userId) {
        return userServiceWebClient.get()
                .uri("/api/users/{userId}/credit", userId)
                .header("X-Internal-Service", internalServiceSecret)
                .retrieve()
                .bodyToMono(CreditAccountResponse.class)
                .block(); // Synchronous call for simple workflow
    }

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public CreditAccountResponse deductCredit(UUID userId, BigDecimal amount) {
        return userServiceWebClient.post()
                .uri("/api/users/{userId}/credit/deduct", userId)
                .header("X-Internal-Service", internalServiceSecret)
                .bodyValue(new CreditDeductRequest(amount))
                .retrieve()
                .bodyToMono(CreditAccountResponse.class)
                .block();
    }

    @CircuitBreaker(name = "userService")
    @Retry(name = "userService")
    public CreditAccountResponse restoreCredit(UUID userId, BigDecimal amount) {
        return userServiceWebClient.post()
                .uri("/api/users/{userId}/credit/restore", userId)
                .header("X-Internal-Service", internalServiceSecret)
                .bodyValue(new CreditDeductRequest(amount))
                .retrieve()
                .bodyToMono(CreditAccountResponse.class)
                .block();
    }
}
