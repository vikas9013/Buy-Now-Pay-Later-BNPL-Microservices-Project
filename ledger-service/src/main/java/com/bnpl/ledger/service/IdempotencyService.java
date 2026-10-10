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

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate, @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        if (objectMapper != null) {
            this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        } else {
            this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String KEY_PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24; // Cache idempotency results for 24 hours

    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        Boolean hasKey = redisTemplate.hasKey(KEY_PREFIX + idempotencyKey);
        return Boolean.TRUE.equals(hasKey);
    }

    public <T> T getResponse(String idempotencyKey, Class<T> responseType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        Object cachedJson = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        if (cachedJson != null) {
            try {
                log.info("⚡ Idempotency Cache HIT for key: {}. Returning cached result instantly!", idempotencyKey);
                return objectMapper.readValue(cachedJson.toString(), responseType);
            } catch (Exception e) {
                log.error("Failed to deserialize cached idempotency response: {}", e.getMessage());
            }
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
            log.info("💾 Cached Idempotency Result in Redis. Key: {}, TTL: {} hours", idempotencyKey, TTL_HOURS);
        } catch (Exception e) {
            log.error("Failed to cache idempotency response in Redis: {}", e.getMessage());
        }
    }
}
