package com.edutech.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Injects X-Request-ID on every inbound request.
 *
 * Uses ServerHttpRequestDecorator instead of request.mutate().header() because
 * Reactor Netty exposes incoming headers as ReadOnlyHttpHeaders, and the
 * DefaultServerHttpRequestBuilder.header() call ultimately delegates put() to
 * the read-only map, throwing UnsupportedOperationException (Spring Web 6.1.x).
 * The decorator overrides getHeaders() with a fresh, writable HttpHeaders copy.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return -2; // runs before JwtAuthenticationFilter (order -1)
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String existing = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        if (existing != null && !existing.isBlank()) {
            return chain.filter(exchange);
        }

        String requestId = UUID.randomUUID().toString();
        ServerHttpRequest decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(super.getHeaders());
                headers.set("X-Request-ID", requestId);
                return headers;
            }
        };
        return chain.filter(exchange.mutate().request(decorated).build());
    }
}
