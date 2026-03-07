// src/main/java/com/edutech/auth/infrastructure/security/Argon2PasswordHasherAdapter.java
package com.edutech.auth.infrastructure.security;

import com.edutech.auth.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter: implements the PasswordHasher domain port using Spring Security's
 * Argon2PasswordEncoder. Parameters are loaded from Argon2Properties.
 */
@Component
public class Argon2PasswordHasherAdapter implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;

    public Argon2PasswordHasherAdapter(Argon2PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean verify(String rawPassword, String storedHash) {
        return encoder.matches(rawPassword, storedHash);
    }
}
