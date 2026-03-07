// src/main/java/com/edutech/auth/domain/port/out/AccessTokenGenerator.java
package com.edutech.auth.domain.port.out;

import com.edutech.auth.domain.model.User;

/**
 * Outbound port for generating signed JWT access tokens.
 * Decouples the application layer from the JWT library (JJWT).
 */
public interface AccessTokenGenerator {
    String generate(User user, String deviceFingerprintHash);
}
