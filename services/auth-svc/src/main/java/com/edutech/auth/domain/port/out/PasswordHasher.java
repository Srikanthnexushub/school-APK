// src/main/java/com/edutech/auth/domain/port/out/PasswordHasher.java
package com.edutech.auth.domain.port.out;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean verify(String rawPassword, String storedHash);
}
