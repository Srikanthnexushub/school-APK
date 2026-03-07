// src/main/java/com/edutech/center/infrastructure/security/JwtTokenValidator.java
package com.edutech.center.infrastructure.security;

import com.edutech.center.application.config.JwtProperties;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates RS256 JWT access tokens issued by auth-svc.
 * Loads the public key once at startup. No signing — validation only.
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtProperties jwtProperties;
    private RSAPublicKey publicKey;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void loadPublicKey() {
        try {
            this.publicKey = readPublicKey(jwtProperties.publicKeyPath());
            log.info("JWT public key loaded from: {}", jwtProperties.publicKeyPath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key from: "
                + jwtProperties.publicKeyPath(), e);
        }
    }

    public Optional<AuthPrincipal> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            Role role = Role.valueOf(claims.get("role", String.class));
            String centerIdStr = claims.get("centerId", String.class);
            UUID centerId = centerIdStr != null ? UUID.fromString(centerIdStr) : null;
            String deviceFP = claims.get("deviceFP", String.class);

            return Optional.of(new AuthPrincipal(userId, email, role, centerId, deviceFP));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private RSAPublicKey readPublicKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = Files.readString(Path.of(path))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }
}
