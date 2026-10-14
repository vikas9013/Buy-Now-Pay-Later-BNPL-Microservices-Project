package com.bnpl.ledger.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Value("${internal.service.secret:bnpl-internal-system-secret-2026}")
    private String internalServiceSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Check for Trusted Inter-Service Secret (Internal workers / Webhooks / System calls)
        String internalHeader = request.getHeader("X-Internal-Service");
        if (internalHeader != null && internalHeader.equals(internalServiceSecret)) {
            UsernamePasswordAuthenticationToken internalAuth = new UsernamePasswordAuthenticationToken(
                    "internal-system-client", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
            );
            internalAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(internalAuth);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check for Gateway Forwarded Verified User Header
        String gatewayUserId = request.getHeader("X-User-Id");
        if (gatewayUserId != null && !gatewayUserId.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID parsedId = UUID.fromString(gatewayUserId);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        parsedId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                filterChain.doFilter(request, response);
                return;
            } catch (IllegalArgumentException ignored) {
                // Fall through to bearer token validation
            }
        }

        // 3. Direct Authorization Bearer Token Check
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String jwtToken = authHeader.substring(7);

            if (jwtUtil.validateToken(jwtToken) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UUID userId = jwtUtil.extractUserId(jwtToken);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (Exception e) {
                    log.warn("Failed to extract userId from Bearer token in ledger-service: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
