package com.edutech.auth.application.service;

import com.edutech.auth.application.config.JwtProperties;
import com.edutech.auth.domain.port.in.GetJwksUseCase;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class JwksService implements GetJwksUseCase {

    private final JwtProperties jwtProperties;
    private Map<String, Object> cachedJwks;

    public JwksService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void loadPublicKey() {
        try {
            RSAPublicKey publicKey = readPublicKey(jwtProperties.publicKeyPath());
            this.cachedJwks = buildJwks(publicKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key from: " + jwtProperties.publicKeyPath(), e);
        }
    }

    @Override
    public Map<String, Object> getJwks() {
        return cachedJwks;
    }

    private RSAPublicKey readPublicKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = Files.readString(Path.of(path))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private Map<String, Object> buildJwks(RSAPublicKey key) {
        // Convert RSA public key to JWK Set format (RFC 7517)
        String n = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(key.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(key.getPublicExponent().toByteArray());

        Map<String, Object> jwk = Map.of(
            "kty", "RSA",
            "use", "sig",
            "alg", "RS256",
            "kid", "edutech-auth-key-1",
            "n", n,
            "e", e
        );
        return Map.of("keys", List.of(jwk));
    }
}
