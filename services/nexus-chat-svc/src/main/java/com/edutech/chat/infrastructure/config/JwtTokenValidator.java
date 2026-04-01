package com.edutech.chat.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final JwtProperties jwtProperties;
    private RSAPublicKey publicKey;

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
            String role = claims.get("role", String.class);
            String centerIdStr = claims.get("centerId", String.class);
            UUID centerId = centerIdStr != null ? UUID.fromString(centerIdStr) : null;

            return Optional.of(new AuthPrincipal(userId, email, role, centerId));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private RSAPublicKey readPublicKey(String path)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = Files.readString(Path.of(path))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(der));
    }
}
