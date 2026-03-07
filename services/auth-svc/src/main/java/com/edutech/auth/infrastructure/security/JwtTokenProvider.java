// src/main/java/com/edutech/auth/infrastructure/security/JwtTokenProvider.java
package com.edutech.auth.infrastructure.security;

import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.AccessTokenGenerator;
import com.edutech.auth.application.config.JwtProperties;
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
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT access token generator (RS256).
 * Implements the AccessTokenGenerator port.
 * Private key is loaded once at startup from the configured PEM file path.
 */
@Component
public class JwtTokenProvider implements AccessTokenGenerator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private RSAPrivateKey privateKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void loadPrivateKey() {
        try {
            this.privateKey = readPrivateKey(jwtProperties.privateKeyPath());
            log.info("JWT private key loaded from: {}", jwtProperties.privateKeyPath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT private key from: "
                + jwtProperties.privateKeyPath(), e);
        }
    }

    @Override
    public String generate(User user, String deviceFingerprintHash) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.accessTokenExpirySeconds());

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim("centerId",
                user.getCenterId() != null ? user.getCenterId().toString() : null)
            .claim("deviceFP", deviceFingerprintHash)
            .id(UUID.randomUUID().toString())
            .issuer(jwtProperties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(privateKey)
            .compact();
    }

    private RSAPrivateKey readPrivateKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = Files.readString(Path.of(path))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
