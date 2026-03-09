package com.edutech.aigateway.infrastructure.security;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final String SERVICE_KEY_HEADER = "X-Service-Key";

    private final JwtTokenValidator jwtTokenValidator;
    private final String serviceApiKey;

    public JwtAuthenticationWebFilter(JwtTokenValidator jwtTokenValidator,
                                      @Value("${service.api-key}") String serviceApiKey) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.serviceApiKey = serviceApiKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Service-to-service: internal callers (parent-svc, ai-mentor-svc) use X-Service-Key
        String serviceKey = exchange.getRequest().getHeaders().getFirst(SERVICE_KEY_HEADER);
        if (serviceKey != null && serviceKey.equals(serviceApiKey)) {
            AuthPrincipal svcPrincipal = new AuthPrincipal(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "service@internal",
                    com.edutech.aigateway.domain.model.Role.SUPER_ADMIN,
                    null,
                    "service"
            );
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    svcPrincipal, null,
                    List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
            );
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        try {
            AuthPrincipal principal = jwtTokenValidator.validate(token);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
            );
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
