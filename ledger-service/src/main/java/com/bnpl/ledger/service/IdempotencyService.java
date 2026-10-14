package com.bnpl.ledger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "idempotency:response:";
    private static final String LOCK_PREFIX = "idempotency:lock:";
    private static final long TTL_HOURS = 24; // Cache idempotency results for 24 hours
    private static final long IN_FLIGHT_LOCK_SECONDS = 30; // Max time for an in-flight request to complete

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate, @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        if (objectMapper != null) {
            this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        } else {
            this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        try {
            Boolean hasKey = redisTemplate.hasKey(KEY_PREFIX + idempotencyKey);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("Redis unavailable for isDuplicate check: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Atomically tries to acquire an in-flight execution lock for the given idempotency key.
     * Prevents duplicate concurrent executions from starting simultaneously.
     *
     * @return true if acquired successfully (first request), false if another concurrent request is currently in-flight
     */
    public boolean tryAcquireLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_PREFIX + idempotencyKey, "PROCESSING", IN_FLIGHT_LOCK_SECONDS, TimeUnit.SECONDS);
            boolean success = Boolean.TRUE.equals(acquired);
            if (!success) {
                log.warn("⚠️ Concurrent duplicate request blocked by Idempotency Lock for key: {}", idempotencyKey);
            }
            return success;
        } catch (Exception e) {
            log.warn("Redis unavailable during tryAcquireLock; proceeding without distributed lock: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Releases the in-flight execution lock without caching a response (e.g. if the request failed).
     */
    public void releaseLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(LOCK_PREFIX + idempotencyKey);
            log.debug("🔓 Released in-flight idempotency lock for key: {}", idempotencyKey);
        } catch (Exception e) {
            log.warn("Failed to release idempotency lock: {}", e.getMessage());
        }
    }

    public <T> T getResponse(String idempotencyKey, Class<T> responseType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        try {
            Object cachedJson = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (cachedJson != null) {
                log.info("⚡ Idempotency Cache HIT for key: {}. Returning cached result instantly!", idempotencyKey);
                return objectMapper.readValue(cachedJson.toString(), responseType);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve or deserialize cached idempotency response: {}", e.getMessage());
        }
        return null;
    }

    public void saveResponse(String idempotencyKey, Object responseObj) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(responseObj);
            redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, json, TTL_HOURS, TimeUnit.HOURS);
            // Once saved, release the in-flight processing lock
            redisTemplate.delete(LOCK_PREFIX + idempotencyKey);
            log.info("💾 Cached Idempotency Result in Redis. Key: {}, TTL: {} hours", idempotencyKey, TTL_HOURS);
        } catch (Exception e) {
            log.error("Failed to cache idempotency response in Redis: {}", e.getMessage());
        }
    }
}
