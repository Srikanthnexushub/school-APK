package com.edutech.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * JWT GlobalFilter: validates Bearer tokens on protected routes and injects
 * X-User-Id / X-User-Role headers for downstream services.
 *
 * Uses ServerHttpRequestDecorator (not request.mutate().header/headers()) to
 * avoid UnsupportedOperationException from ReadOnlyHttpHeaders — Reactor Netty
 * surfaces incoming request headers as read-only in Spring Web 6.1.x.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/v1/otp/",
            "/api/v1/captcha/",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars"
    );

    /** Headers clients must never be allowed to inject. */
    private static final Set<String> STRIPPED_HEADERS = Set.of(
            "X-User-Id", "X-User-Role", "X-User-Center-Id"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublic(path)) {
            return chain.filter(exchange.mutate().request(stripped(exchange.getRequest())).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtTokenValidator.validate(token);
            ServerHttpRequest enriched = withUserHeaders(exchange.getRequest(), claims);
            return chain.filter(exchange.mutate().request(enriched).build());
        } catch (JwtException e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Returns a decorator whose getHeaders() omits all STRIPPED_HEADERS.
     * Avoids mutating ReadOnlyHttpHeaders from Reactor Netty.
     */
    private ServerHttpRequest stripped(ServerHttpRequest request) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                super.getHeaders().forEach((name, values) -> {
                    if (!STRIPPED_HEADERS.contains(name)) {
                        headers.put(name, values);
                    }
                });
                return headers;
            }
        };
    }

    /**
     * Returns a decorator that strips injected headers and adds validated JWT claims.
     */
    private ServerHttpRequest withUserHeaders(ServerHttpRequest request, Claims claims) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                super.getHeaders().forEach((name, values) -> {
                    if (!STRIPPED_HEADERS.contains(name)) {
                        headers.put(name, values);
                    }
                });
                headers.set("X-User-Id", claims.getSubject());
                String role = claims.get("role", String.class);
                if (role != null) headers.set("X-User-Role", role);
                String centerId = claims.get("centerId", String.class);
                if (centerId != null) headers.set("X-User-Center-Id", centerId);
                return headers;
            }
        };
    }
}
