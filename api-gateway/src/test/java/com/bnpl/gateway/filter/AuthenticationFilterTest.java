package com.bnpl.gateway.filter;

import com.bnpl.gateway.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    @Test
    @DisplayName("Open endpoint (/api/auth/register) should bypass token validation")
    void testOpenEndpoint_BypassesAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);
        assertNotNull(result);
        verify(filterChain, times(1)).filter(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Secured endpoint without Authorization header should return 401 Unauthorized")
    void testSecuredEndpoint_MissingAuthHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/ledger/checkout").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Secured endpoint with valid Bearer token should append X-User-Id and pass downstream")
    void testSecuredEndpoint_ValidToken_AppendsUserIdAndPasses() {
        UUID userId = UUID.randomUUID();
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/ledger/checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token.here")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken("valid.token.here")).thenReturn(true);
        when(jwtUtil.extractUserId("valid.token.here")).thenReturn(userId);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);
        assertNotNull(result);
        verify(filterChain, times(1)).filter(any());
    }
}
