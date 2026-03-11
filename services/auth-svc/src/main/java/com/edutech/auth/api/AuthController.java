// src/main/java/com/edutech/auth/api/AuthController.java
package com.edutech.auth.api;

import com.edutech.auth.application.dto.AuthPrincipal;
import com.edutech.auth.application.dto.LoginRequest;
import com.edutech.auth.application.dto.RefreshTokenRequest;
import com.edutech.auth.application.dto.RegisterRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.dto.UserResponse;
import com.edutech.auth.api.mapper.AuthMapper;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.AuthenticateUserUseCase;
import com.edutech.auth.domain.port.in.LogoutUseCase;
import com.edutech.auth.domain.port.in.RefreshTokenUseCase;
import com.edutech.auth.domain.port.in.RegisterUserUseCase;
import com.edutech.auth.domain.port.out.UserRepository;
import com.edutech.auth.infrastructure.security.TrustedProxyValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, token refresh, logout")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final TrustedProxyValidator trustedProxyValidator;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          UserRepository userRepository,
                          AuthMapper authMapper,
                          TrustedProxyValidator trustedProxyValidator) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.userRepository = userRepository;
        this.authMapper = authMapper;
        this.trustedProxyValidator = trustedProxyValidator;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user account")
    public TokenPair register(@Valid @RequestBody RegisterRequest request,
                              HttpServletRequest servletRequest) {
        return registerUserUseCase.register(
            request,
            getClientIp(servletRequest),
            servletRequest.getHeader("User-Agent")
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a token pair")
    public TokenPair login(@Valid @RequestBody LoginRequest request,
                           HttpServletRequest servletRequest) {
        return authenticateUserUseCase.authenticate(
            request,
            getClientIp(servletRequest),
            servletRequest.getHeader("User-Agent")
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token — single-use, device-bound")
    public TokenPair refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshTokenUseCase.refresh(
            request.refreshToken(),
            request.deviceFingerprint()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Revoke current session refresh token")
    public void logout(@Valid @RequestBody RefreshTokenRequest request,
                       @AuthenticationPrincipal AuthPrincipal principal) {
        logoutUseCase.logout(request.refreshToken(), principal.userId());
    }

    @PostMapping("/logout/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Revoke all active sessions for this user")
    public void logoutAll(@AuthenticationPrincipal AuthPrincipal principal) {
        logoutUseCase.logoutAllDevices(principal.userId());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current authenticated user profile")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = userRepository.findById(principal.userId())
            .orElseThrow();
        return authMapper.toUserResponse(user);
    }

    @GetMapping("/users/lookup")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Look up a user by email — used by parents to find their child's account")
    public ResponseEntity<UserResponse> lookupByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
            .map(authMapper::toUserResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxyValidator.isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // Take the first IP (client's real IP) from the chain
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
