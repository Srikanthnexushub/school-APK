// src/main/java/com/edutech/auth/infrastructure/security/SecurityConfig.java
package com.edutech.auth.infrastructure.security;

import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.infrastructure.config.Argon2Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenValidator jwtTokenValidator;
    private final Argon2Properties argon2Properties;

    public SecurityConfig(JwtTokenValidator jwtTokenValidator,
                          Argon2Properties argon2Properties) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.argon2Properties = argon2Properties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/captcha/challenge",
                    "/api/v1/auth/jwks").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/otp/send",
                    "/api/v1/otp/verify").permitAll()
                // Health + docs (internal/dev access)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenValidator);
    }

    /**
     * Argon2id password encoder with parameters from config.
     * Exposed as a PasswordHasher port implementation via Argon2PasswordHasherAdapter.
     */
    @Bean
    public Argon2PasswordEncoder argon2PasswordEncoder() {
        return new Argon2PasswordEncoder(
            argon2Properties.saltLength(),
            argon2Properties.hashLength(),
            argon2Properties.parallelism(),
            argon2Properties.memoryCost(),
            argon2Properties.iterations()
        );
    }
}
