package com.edutech.studentgateway.filter;

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
        // Use ServerHttpRequestDecorator to avoid UnsupportedOperationException on ReadOnlyHttpHeaders
        HttpHeaders mutableHeaders = new HttpHeaders();
        mutableHeaders.addAll(exchange.getRequest().getHeaders());
        mutableHeaders.set("X-Request-ID", requestId);
        ServerHttpRequest decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return mutableHeaders;
            }
        };
        return chain.filter(exchange.mutate().request(decorated).build());
    }
}
