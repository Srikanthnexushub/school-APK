package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.GoogleAuthRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.AuthException;
import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Handles Google ID-token exchange.
 *
 * Flow:
 *  1. Frontend uses @react-oauth/google to obtain a Google credential (ID token).
 *  2. Frontend posts { idToken } to POST /api/v1/auth/google.
 *  3. This service verifies the token via Google's tokeninfo endpoint.
 *  4. Finds or creates the local user (provider=GOOGLE, emailVerified=true).
 *  5. Issues a JWT token pair via TokenService.
 */
@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String USERINFO_URL =
        "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final String googleClientId;

    public GoogleOAuthService(UserRepository userRepository,
                              TokenService tokenService,
                              ObjectMapper objectMapper,
                              @Value("${google.client-id:}") String googleClientId) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.googleClientId = googleClientId;
    }

    @Transactional
    public TokenPair authenticate(GoogleAuthRequest request,
                                  String userAgent,
                                  String deviceId) {
        if (googleClientId.isBlank()) {
            throw new GoogleOAuthDisabledException();
        }

        GoogleTokenPayload payload = verifyIdToken(request.idToken());

        if (!payload.emailVerified()) {
            throw new InvalidGoogleTokenException("Google account email is not verified");
        }

        // Find existing user by Google sub or email
        User user = userRepository
            .findByProviderAndProviderId("GOOGLE", payload.sub())
            .or(() -> userRepository.findByEmail(payload.email()))
            .orElseGet(() -> createOAuthUser(payload, request.role()));

        // If found by email but with no provider, link the Google account
        if (user.getProvider() == null) {
            user.linkOAuthProvider("GOOGLE", payload.sub());
            userRepository.save(user);
        }

        DeviceFingerprint fingerprint = new DeviceFingerprint(
            userAgent != null ? userAgent : "unknown",
            deviceId != null ? deviceId : "google-oauth",
            "127.0.0"
        );

        log.info("[GoogleOAuth] login/register success: email={} sub={}", payload.email(), payload.sub());
        return tokenService.issueTokenPair(user, fingerprint);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private GoogleTokenPayload verifyIdToken(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                log.warn("[GoogleOAuth] userinfo returned status={}", res.statusCode());
                throw new InvalidGoogleTokenException("Google token verification failed");
            }

            JsonNode json = objectMapper.readTree(res.body());

            if (json.has("error")) {
                throw new InvalidGoogleTokenException(
                    "Google token error: " + json.path("error_description").asText(json.path("error").asText()));
            }

            return new GoogleTokenPayload(
                json.path("sub").asText(),
                json.path("email").asText(),
                json.path("given_name").asText(""),
                json.path("family_name").asText(""),
                "true".equalsIgnoreCase(json.path("email_verified").asText())
            );
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidGoogleTokenException("Failed to reach Google userinfo endpoint");
        }
    }

    private User createOAuthUser(GoogleTokenPayload payload, String requestedRole) {
        Role role;
        try {
            role = (requestedRole != null && !requestedRole.isBlank())
                ? Role.valueOf(requestedRole.toUpperCase())
                : Role.STUDENT;
        } catch (IllegalArgumentException ex) {
            role = Role.STUDENT;
        }

        String firstName = payload.givenName().isBlank() ? "Google" : payload.givenName();
        String lastName = payload.familyName().isBlank() ? "User" : payload.familyName();

        return userRepository.save(
            User.createFromOAuth(
                payload.email(), firstName, lastName,
                "GOOGLE", payload.sub(), role, null
            )
        );
    }

    // ── Embedded types ─────────────────────────────────────────────────────

    private record GoogleTokenPayload(
        String sub,
        String email,
        String givenName,
        String familyName,
        boolean emailVerified
    ) {}

    public static class GoogleOAuthDisabledException extends AuthException {
        public GoogleOAuthDisabledException() {
            super("Google Sign-In is not configured on this server");
        }
    }

    public static class InvalidGoogleTokenException extends AuthException {
        public InvalidGoogleTokenException(String message) {
            super(message);
        }
    }
}
