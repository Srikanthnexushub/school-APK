package com.edutech.aigateway.infrastructure.security;

import com.edutech.aigateway.application.config.JwtProperties;
import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
public class JwtTokenValidator {

    private final RSAPublicKey publicKey;
    private final String issuer;

    public JwtTokenValidator(JwtProperties props) throws Exception {
        this.issuer = props.issuer();

        String pem = new String(
                java.nio.file.Files.readAllBytes(java.nio.file.Path.of(props.publicKeyPath()))
        );
        String stripped = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(stripped);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.publicKey = (RSAPublicKey) kf.generatePublic(spec);
    }

    public AuthPrincipal validate(String token) {
        Claims payload = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(payload.getSubject());
        String email = payload.get("email", String.class);
        Role role = Role.valueOf(payload.get("role", String.class));

        String centerIdStr = payload.get("centerId", String.class);
        UUID centerId = (centerIdStr != null && !centerIdStr.isBlank())
                ? UUID.fromString(centerIdStr)
                : null;

        String deviceFP = payload.get("deviceFP", String.class);

        return new AuthPrincipal(userId, email, role, centerId, deviceFP);
    }
}
