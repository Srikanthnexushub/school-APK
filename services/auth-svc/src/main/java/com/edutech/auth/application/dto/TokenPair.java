// src/main/java/com/edutech/auth/application/dto/TokenPair.java
package com.edutech.auth.application.dto;

import java.time.Instant;

public record TokenPair(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    String tokenType
) {
    public TokenPair(String accessToken, String refreshToken,
                     Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {
        this(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt, "Bearer");
    }
}
