package com.edutech.gateway.security;

import com.edutech.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates JWT RS256 tokens.
 *
 * On startup and periodically (every jwksCacheTtlSeconds), fetches the JWKS
 * from auth-svc. Falls back to the static public-key file if the JWKS endpoint
 * is unavailable.
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtProperties jwtProperties;
    private final WebClient webClient;

    /** Cached public key and the time it was last refreshed. */
    private final AtomicReference<RSAPublicKey> publicKeyRef = new AtomicReference<>();
    private volatile Instant lastRefreshed = Instant.EPOCH;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.webClient = WebClient.builder().build();
    }

    @PostConstruct
    void init() {
        refreshPublicKey();
    }

    public Claims validate(String token) {
        refreshIfStale();
        RSAPublicKey key = publicKeyRef.get();
        if (key == null) {
            throw new JwtException("No JWT public key available");
        }
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // -------------------------------------------------------------------------
    // Key refresh logic
    // -------------------------------------------------------------------------

    private void refreshIfStale() {
        long ttl = jwtProperties.jwksCacheTtlSeconds();
        if (Instant.now().isAfter(lastRefreshed.plusSeconds(ttl))) {
            refreshPublicKey();
        }
    }

    private synchronized void refreshPublicKey() {
        try {
            RSAPublicKey key = fetchFromJwks();
            publicKeyRef.set(key);
            lastRefreshed = Instant.now();
            log.info("JWT public key refreshed from JWKS endpoint");
        } catch (Exception jwksEx) {
            log.warn("Failed to fetch JWKS from {}: {}. Falling back to static key file.",
                    jwtProperties.jwksUri(), jwksEx.getMessage());
            try {
                RSAPublicKey key = loadStaticPublicKey(jwtProperties.publicKeyPath());
                publicKeyRef.set(key);
                lastRefreshed = Instant.now();
                log.info("JWT public key loaded from static file: {}", jwtProperties.publicKeyPath());
            } catch (Exception fileEx) {
                log.error("Failed to load JWT public key from static file: {}", fileEx.getMessage(), fileEx);
                // Do not overwrite existing key if we already have one
                if (publicKeyRef.get() == null) {
                    throw new IllegalStateException("No JWT public key could be loaded", fileEx);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private RSAPublicKey fetchFromJwks() {
        Map<String, Object> jwks = webClient.get()
                .uri(jwtProperties.jwksUri())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (jwks == null) {
            throw new IllegalStateException("JWKS response was null");
        }

        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("No keys found in JWKS response");
        }

        // Use the first RS256 key (or the only key if there is just one)
        Map<String, Object> jwk = keys.stream()
                .filter(k -> "RSA".equals(k.get("kty")) && "RS256".equals(k.get("alg")))
                .findFirst()
                .orElse(keys.get(0));

        String nB64 = (String) jwk.get("n");
        String eB64 = (String) jwk.get("e");
        if (nB64 == null || eB64 == null) {
            throw new IllegalStateException("JWK missing 'n' or 'e' fields");
        }

        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(nB64));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eB64));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (java.security.NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to construct RSA public key from JWKS", e);
        }
    }

    private RSAPublicKey loadStaticPublicKey(String path) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(path));
        String pem = new String(keyBytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }
}
