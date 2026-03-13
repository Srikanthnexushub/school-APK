// src/main/java/com/edutech/auth/domain/port/out/OtpStore.java
package com.edutech.auth.domain.port.out;

import java.util.Optional;

public interface OtpStore {
    void save(String key, String otp, int ttlSeconds);
    Optional<String> find(String key);
    void incrementAttempts(String key);
    int getAttempts(String key);
    void delete(String key);
    int getResends(String key);
    void incrementResends(String key, int ttlSeconds);
}
