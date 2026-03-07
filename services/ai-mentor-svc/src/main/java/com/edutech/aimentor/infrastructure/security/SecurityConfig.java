package com.edutech.aimentor.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for ai-mentor-svc.
 *
 * This service sits behind the student-gateway, which validates JWTs and injects
 * X-User-Id and X-User-Role headers. No JWT re-validation is performed here.
 * All requests with these trusted headers are accepted as authenticated.
 *
 * Network-level security (only traffic from the gateway reaches this service)
 * is enforced via the deployment topology (Kubernetes NetworkPolicy / service mesh).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/metrics",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().permitAll()  // Gateway has already authenticated the request
                )
                .build();
    }
}
