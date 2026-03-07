package com.edutech.gateway.security;

import com.edutech.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenValidator Unit Tests")
class JwtTokenValidatorTest {

    static JwtTokenValidator validator;
    static RSAPrivateKey privateKey;
    static final String ISSUER = "https://edutech.com/auth";

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();

        // Write PEM public key to temp file
        String b64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----\n";
        Path tempFile = Files.createTempFile("jwt-pub-", ".pem");
        Files.writeString(tempFile, pem);
        tempFile.toFile().deleteOnExit();

        JwtProperties props = new JwtProperties(tempFile.toString(), ISSUER, null);
        validator = new JwtTokenValidator(props);
    }

    private String signedToken(String subject, String issuer, long expiryOffsetMs) {
        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .expiration(new Date(System.currentTimeMillis() + expiryOffsetMs))
                .claim("role", "CENTER_ADMIN")
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Test
    @DisplayName("validate_success: valid token returns claims with correct subject")
    void validate_success() {
        String token = signedToken("user-123", ISSUER, 60_000);
        Claims claims = validator.validate(token);
        assertEquals("user-123", claims.getSubject());
        assertEquals(ISSUER, claims.getIssuer());
        assertEquals("CENTER_ADMIN", claims.get("role", String.class));
    }

    @Test
    @DisplayName("validate_expiredToken: expired token throws JwtException")
    void validate_expiredToken() {
        String token = signedToken("user-123", ISSUER, -1000); // already expired
        assertThrows(JwtException.class, () -> validator.validate(token));
    }

    @Test
    @DisplayName("validate_wrongIssuer: wrong issuer throws JwtException")
    void validate_wrongIssuer() {
        String token = signedToken("user-123", "https://evil.com", 60_000);
        assertThrows(JwtException.class, () -> validator.validate(token));
    }

    @Test
    @DisplayName("validate_tamperedToken: tampered token throws JwtException")
    void validate_tamperedToken() {
        String token = signedToken("user-123", ISSUER, 60_000);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThrows(JwtException.class, () -> validator.validate(tampered));
    }

    @Test
    @DisplayName("validate_emptyToken: blank token throws JwtException")
    void validate_emptyToken() {
        assertThrows(Exception.class, () -> validator.validate("not.a.token"));
    }
}
