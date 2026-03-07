package com.edutech.psych.infrastructure.security;

import com.edutech.psych.application.config.JwtProperties;
import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
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
        String pem = Files.readString(Paths.get(props.publicKeyPath()))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        this.publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        this.issuer = props.issuer();
    }

    public AuthPrincipal validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        Role role = Role.valueOf(claims.get("role", String.class));
        String centerIdStr = claims.get("centerId", String.class);
        UUID centerId = centerIdStr != null ? UUID.fromString(centerIdStr) : null;
        String deviceFP = claims.get("deviceFP", String.class);

        return new AuthPrincipal(userId, email, role, centerId, deviceFP);
    }
}
