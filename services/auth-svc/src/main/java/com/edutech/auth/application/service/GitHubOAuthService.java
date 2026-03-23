package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.GitHubAuthRequest;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Handles GitHub OAuth2 authorization code exchange.
 *
 * Flow:
 *  1. Frontend redirects user to GitHub's OAuth authorize page.
 *  2. GitHub redirects back with ?code=&state= query params.
 *  3. Frontend detects params and posts { code } to POST /api/v1/auth/github.
 *  4. This service exchanges the code for an access token via GitHub's token endpoint.
 *  5. Uses the access token to fetch the user's GitHub profile.
 *  6. Finds or creates the local user (provider=GITHUB, emailVerified=true).
 *  7. Issues a JWT token pair via TokenService.
 */
@Service
public class GitHubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);
    private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_API_URL = "https://api.github.com/user";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;

    public GitHubOAuthService(UserRepository userRepository,
                               TokenService tokenService,
                               ObjectMapper objectMapper,
                               @Value("${github.client-id:}") String clientId,
                               @Value("${github.client-secret:}") String clientSecret) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Transactional
    public TokenPair authenticate(GitHubAuthRequest request, String userAgent, String deviceId) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new GitHubOAuthDisabledException();
        }

        String accessToken = exchangeCodeForToken(request.code());
        GitHubUserPayload payload = fetchUserProfile(accessToken);

        String providerId = String.valueOf(payload.id());

        User user = userRepository
            .findByProviderAndProviderId("GITHUB", providerId)
            .or(() -> userRepository.findByEmail(payload.email()))
            .orElseGet(() -> createOAuthUser(payload, request.role()));

        // If found by email but not yet linked to GitHub, link the account
        if (user.getProvider() == null) {
            user.linkOAuthProvider("GITHUB", providerId);
            userRepository.save(user);
        }

        DeviceFingerprint fingerprint = new DeviceFingerprint(
            userAgent != null ? userAgent : "unknown",
            deviceId != null ? deviceId : "github-oauth",
            "127.0.0"
        );

        log.info("[GitHubOAuth] login/register success: email={} githubId={}", payload.email(), payload.id());
        return tokenService.issueTokenPair(user, fingerprint);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String exchangeCodeForToken(String code) {
        try {
            String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ACCESS_TOKEN_URL))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("[GitHubOAuth] token exchange returned status={}", res.statusCode());
                throw new InvalidGitHubCodeException("GitHub token exchange failed: HTTP " + res.statusCode());
            }

            JsonNode json = objectMapper.readTree(res.body());
            if (json.has("error")) {
                throw new InvalidGitHubCodeException(
                    "GitHub token error: " + json.path("error_description").asText(json.path("error").asText()));
            }

            String token = json.path("access_token").asText("");
            if (token.isBlank()) {
                throw new InvalidGitHubCodeException("GitHub returned empty access token");
            }
            return token;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidGitHubCodeException("Failed to reach GitHub token endpoint");
        }
    }

    private GitHubUserPayload fetchUserProfile(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USER_API_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("[GitHubOAuth] user API returned status={}", res.statusCode());
                throw new InvalidGitHubCodeException("GitHub user profile fetch failed");
            }

            JsonNode json = objectMapper.readTree(res.body());
            long id = json.path("id").asLong();
            String login = json.path("login").asText("github_user");

            // Email may be null when user keeps it private — fall back to noreply address
            JsonNode emailNode = json.path("email");
            String email = (!emailNode.isNull() && !emailNode.isMissingNode())
                ? emailNode.asText("").strip()
                : "";
            if (email.isBlank()) {
                email = login + "@users.noreply.github.com";
            }

            // name may be null for accounts with no display name set
            String fullName = json.path("name").asText("").strip();
            String firstName;
            String lastName;
            if (fullName.isBlank()) {
                firstName = login;
                lastName = "";
            } else {
                int space = fullName.indexOf(' ');
                firstName = space > 0 ? fullName.substring(0, space) : fullName;
                lastName = space > 0 ? fullName.substring(space + 1) : "";
            }

            return new GitHubUserPayload(id, login, email, firstName, lastName);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidGitHubCodeException("Failed to reach GitHub user API");
        }
    }

    private User createOAuthUser(GitHubUserPayload payload, String requestedRole) {
        Role role;
        try {
            role = (requestedRole != null && !requestedRole.isBlank())
                ? Role.valueOf(requestedRole.toUpperCase())
                : Role.STUDENT;
        } catch (IllegalArgumentException ex) {
            role = Role.STUDENT;
        }

        String firstName = payload.firstName().isBlank() ? "GitHub" : payload.firstName();
        String lastName = payload.lastName().isBlank() ? "User" : payload.lastName();

        return userRepository.save(
            User.createFromOAuth(
                payload.email(), firstName, lastName,
                "GITHUB", String.valueOf(payload.id()), role, null
            )
        );
    }

    // ── Embedded types ─────────────────────────────────────────────────────

    private record GitHubUserPayload(
        long id,
        String login,
        String email,
        String firstName,
        String lastName
    ) {}

    public static class GitHubOAuthDisabledException extends AuthException {
        public GitHubOAuthDisabledException() {
            super("GitHub Sign-In is not configured on this server");
        }
    }

    public static class InvalidGitHubCodeException extends AuthException {
        public InvalidGitHubCodeException(String message) {
            super(message);
        }
    }
}
