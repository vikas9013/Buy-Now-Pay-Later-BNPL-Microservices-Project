package com.bnpl.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "bnpl-super-secret-security-key-for-jwt-signing-2026-production";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        jwtUtil.init();
    }

    @Test
    @DisplayName("Should successfully generate a valid JWT token")
    void testGenerateToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = jwtUtil.generateToken(userId, email);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(userId, jwtUtil.extractUserId(token));
        assertEquals(email, jwtUtil.extractAllClaims(token).get("email"));
    }

    @Test
    @DisplayName("Should validate unexpired token")
    void testValidateToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "aarav@example.com");

        assertTrue(jwtUtil.validateToken(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }
}
