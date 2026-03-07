// src/main/java/com/edutech/auth/application/dto/DeviceFingerprint.java
package com.edutech.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record DeviceFingerprint(
    @NotBlank String userAgent,
    String deviceId,
    String ipSubnet
) {
    /**
     * Deterministic SHA-256 hash of device attributes.
     * Used to bind refresh tokens to the originating device.
     */
    public String toFingerprintHash() {
        String raw = userAgent + "|" + nullSafe(deviceId) + "|" + nullSafe(ipSubnet);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — unreachable
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
