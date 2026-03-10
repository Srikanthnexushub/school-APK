package com.edutech.psych.infrastructure.security;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.domain.model.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Allows trusted internal services (e.g. parent-svc) to call psych-svc
 * without a user JWT by presenting the shared X-Service-Key header.
 * A synthetic SUPER_ADMIN principal is injected so existing role checks pass.
 */
@Component
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

    private static final String SERVICE_KEY_HEADER = "X-Service-Key";
    private static final UUID SERVICE_CALLER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final String expectedServiceKey;

    public ServiceKeyAuthFilter(@Value("${service.api-key}") String expectedServiceKey) {
        this.expectedServiceKey = expectedServiceKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String serviceKey = request.getHeader(SERVICE_KEY_HEADER);
        if (serviceKey != null && serviceKey.equals(expectedServiceKey)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthPrincipal principal = new AuthPrincipal(
                    SERVICE_CALLER_ID, "service@internal", Role.SUPER_ADMIN, null, null);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
