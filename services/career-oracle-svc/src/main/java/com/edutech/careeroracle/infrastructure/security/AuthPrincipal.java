package com.edutech.careeroracle.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Thin wrapper around the Spring Security context populated by {@link GatewayHeaderAuthFilter}.
 * Provides role-check helpers without leaking Spring Security into the application/domain layers.
 */
public record AuthPrincipal(UUID userId, String role) {

    /**
     * Resolves the current principal from the Spring Security context.
     * Throws {@link IllegalStateException} if no authenticated principal is present.
     */
    public static AuthPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated principal found");
        }
        String rawUserId = auth.getPrincipal().toString();
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("")
                .replace("ROLE_", "");
        return new AuthPrincipal(UUID.fromString(rawUserId), role);
    }

    public boolean isStudent()          { return "STUDENT".equalsIgnoreCase(role); }
    public boolean isParent()           { return "PARENT".equalsIgnoreCase(role); }
    public boolean isTeacher()          { return "TEACHER".equalsIgnoreCase(role); }
    public boolean isCenterAdmin()      { return "CENTER_ADMIN".equalsIgnoreCase(role); }
    public boolean isInstitutionAdmin() { return "INSTITUTION_ADMIN".equalsIgnoreCase(role); }
    public boolean isSuperAdmin()       { return "SUPER_ADMIN".equalsIgnoreCase(role); }
}
