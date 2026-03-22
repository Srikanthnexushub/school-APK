package com.edutech.aigateway.infrastructure.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive correlation ID filter.
 * Injects X-Request-Id into the reactor Context and response headers.
 * traceId/spanId are bridged automatically by micrometer-tracing-bridge-otel.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);
    static final String REQUEST_ID_HEADER = "X-Request-Id";
    static final String CONTEXT_REQUEST_ID = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String rid = requestId;
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, rid);
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CONTEXT_REQUEST_ID, rid));
    }
}
