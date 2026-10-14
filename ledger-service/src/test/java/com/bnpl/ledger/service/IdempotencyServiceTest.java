package com.bnpl.ledger.service;

import com.bnpl.ledger.dto.CheckoutResponse;
import com.bnpl.ledger.enums.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ObjectMapper objectMapper;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        idempotencyService = new IdempotencyService(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should successfully acquire in-flight execution lock for new idempotency key")
    void testTryAcquireLock_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.TRUE);

        boolean acquired = idempotencyService.tryAcquireLock("idemp-key-123");
        assertTrue(acquired);
        verify(valueOperations, times(1)).setIfAbsent(eq("idempotency:lock:idemp-key-123"), eq("PROCESSING"), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should reject in-flight lock when duplicate concurrent request is executing")
    void testTryAcquireLock_DuplicateConcurrentBlocked() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.FALSE);

        boolean acquired = idempotencyService.tryAcquireLock("idemp-key-123");
        assertFalse(acquired);
    }

    @Test
    @DisplayName("Should cache serialized response and release in-flight lock")
    void testSaveResponse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CheckoutResponse response = CheckoutResponse.builder()
                .transactionId(UUID.randomUUID())
                .principalAmount(new BigDecimal("1000.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        idempotencyService.saveResponse("idemp-key-123", response);

        verify(valueOperations, times(1)).set(eq("idempotency:response:idemp-key-123"), anyString(), eq(24L), eq(TimeUnit.HOURS));
        verify(redisTemplate, times(1)).delete("idempotency:lock:idemp-key-123");
    }

    @Test
    @DisplayName("Should retrieve cached response on cache hit")
    void testGetResponse_CacheHit() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CheckoutResponse original = CheckoutResponse.builder()
                .transactionId(UUID.randomUUID())
                .principalAmount(new BigDecimal("1000.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        String json = objectMapper.writeValueAsString(original);
        when(valueOperations.get("idempotency:response:idemp-key-123")).thenReturn(json);

        CheckoutResponse cached = idempotencyService.getResponse("idemp-key-123", CheckoutResponse.class);

        assertNotNull(cached);
        assertEquals(original.getTransactionId(), cached.getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, cached.getStatus());
    }

    @Test
    @DisplayName("Should return null on cache miss")
    void testGetResponse_CacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:response:unknown-key")).thenReturn(null);

        CheckoutResponse cached = idempotencyService.getResponse("unknown-key", CheckoutResponse.class);
        assertNull(cached);
    }
}
