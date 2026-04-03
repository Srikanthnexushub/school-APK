// src/main/java/com/edutech/auth/api/AuthController.java
package com.edutech.auth.api;

import com.edutech.auth.application.dto.AssignUserCenterRequest;
import com.edutech.auth.application.dto.RegisterStaffRequest;
import com.edutech.auth.application.dto.RegisterStaffResponse;
import com.edutech.auth.application.dto.AuthAuditStatsResponse;
import com.edutech.auth.application.dto.AuthPrincipal;
import com.edutech.auth.application.dto.ChangePasswordRequest;
import com.edutech.auth.application.dto.RegisterChildRequest;
import com.edutech.auth.application.dto.RegisterChildResponse;
import com.edutech.auth.application.dto.ForgotPasswordRequest;
import com.edutech.auth.application.dto.GitHubAuthRequest;
import com.edutech.auth.application.dto.GoogleAuthRequest;
import com.edutech.auth.application.dto.LoginRequest;
import com.edutech.auth.application.dto.PasswordResetOtpVerifyResponse;
import com.edutech.auth.application.dto.RefreshTokenRequest;
import com.edutech.auth.application.dto.RegisterRequest;
import com.edutech.auth.application.dto.ResetPasswordRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.dto.UpdateNameRequest;
import com.edutech.auth.application.dto.UserResponse;
import com.edutech.auth.api.mapper.AuthMapper;
import com.edutech.auth.application.service.GitHubOAuthService;
import com.edutech.auth.application.service.GoogleOAuthService;
import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.AuthenticateUserUseCase;
import com.edutech.auth.domain.port.in.ChangePasswordUseCase;
import com.edutech.auth.domain.port.in.LogoutUseCase;
import com.edutech.auth.domain.port.in.PasswordResetUseCase;
import com.edutech.auth.domain.port.in.RefreshTokenUseCase;
import com.edutech.auth.application.service.UserRegistrationService;
import com.edutech.auth.domain.port.in.RegisterUserUseCase;
import com.edutech.auth.domain.port.out.UserRepository;
import com.edutech.auth.infrastructure.security.TrustedProxyValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, token refresh, logout")
@Validated
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final PasswordResetUseCase passwordResetUseCase;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final TrustedProxyValidator trustedProxyValidator;
    private final GoogleOAuthService googleOAuthService;
    private final GitHubOAuthService gitHubOAuthService;
    private final UserRegistrationService userRegistrationService;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          PasswordResetUseCase passwordResetUseCase,
                          UserRepository userRepository,
                          AuthMapper authMapper,
                          TrustedProxyValidator trustedProxyValidator,
                          GoogleOAuthService googleOAuthService,
                          GitHubOAuthService gitHubOAuthService,
                          UserRegistrationService userRegistrationService) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.passwordResetUseCase = passwordResetUseCase;
        this.userRepository = userRepository;
        this.authMapper = authMapper;
        this.trustedProxyValidator = trustedProxyValidator;
        this.googleOAuthService = googleOAuthService;
        this.gitHubOAuthService = gitHubOAuthService;
        this.userRegistrationService = userRegistrationService;
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

    @PostMapping("/register-child")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Register a child account — authenticated parents only, no captcha required")
    public RegisterChildResponse registerChild(@Valid @RequestBody RegisterChildRequest request,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        return userRegistrationService.registerChild(request);
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

    @PatchMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update current user's display name")
    public ResponseEntity<Void> updateMe(@AuthenticationPrincipal AuthPrincipal principal,
                                         @Valid @RequestBody UpdateNameRequest request) {
        User user = userRepository.findById(principal.userId()).orElseThrow();
        user.updateName(request.firstName(), request.lastName());
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Change password — requires current password; invalidates all other sessions")
    public void changePassword(@AuthenticationPrincipal AuthPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordUseCase.changePassword(
            principal.userId(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Initiate password reset — sends a 6-digit OTP to the registered email")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetUseCase.sendPasswordResetOtp(request.email());
    }

    @PostMapping("/verify-reset-otp")
    @Operation(summary = "Verify password reset OTP — returns a short-lived reset token")
    public PasswordResetOtpVerifyResponse verifyResetOtp(@Valid @RequestBody
                                                         com.edutech.auth.application.dto.OtpVerifyRequest request) {
        String resetToken = passwordResetUseCase.verifyPasswordResetOtp(request.email(), request.otp());
        return new PasswordResetOtpVerifyResponse(resetToken);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Complete password reset using the token from verify-reset-otp; invalidates all sessions")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetUseCase.resetPassword(
            request.email(), request.resetToken(), request.newPassword());
    }

    @GetMapping("/users/lookup")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Look up a user by email — used by parents to find their child's account")
    public ResponseEntity<UserResponse> lookupByEmail(@RequestParam @Email @NotBlank String email) {
        return userRepository.findByEmail(email)
            .map(authMapper::toUserResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/register-staff")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin creates a staff auth account — CENTER_ADMIN/SUPER_ADMIN only, no captcha")
    public RegisterStaffResponse registerStaff(@Valid @RequestBody RegisterStaffRequest request,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal.role() != Role.SUPER_ADMIN
                && principal.role() != Role.INSTITUTION_ADMIN
                && principal.role() != Role.CENTER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges");
        }
        return userRegistrationService.registerStaff(request);
    }

    @PatchMapping("/admin/users/{userId}/center")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Assign or reassign a user to a center — CENTER_ADMIN / INSTITUTION_ADMIN / SUPER_ADMIN only")
    public void assignUserCenter(@PathVariable UUID userId,
                                 @Valid @RequestBody AssignUserCenterRequest request,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal.role() != Role.SUPER_ADMIN
                && principal.role() != Role.INSTITUTION_ADMIN
                && principal.role() != Role.CENTER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.assignCenter(request.centerId());
        userRepository.save(user);
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in or register via Google ID token (Google One Tap / OAuth2)")
    public TokenPair googleSignIn(@Valid @RequestBody GoogleAuthRequest request,
                                  HttpServletRequest servletRequest) {
        return googleOAuthService.authenticate(
            request,
            servletRequest.getHeader("User-Agent"),
            servletRequest.getHeader("X-Device-Id")
        );
    }

    @PostMapping("/github")
    @Operation(summary = "Sign in or register via GitHub OAuth2 authorization code")
    public TokenPair githubSignIn(@Valid @RequestBody GitHubAuthRequest request,
                                  HttpServletRequest servletRequest) {
        return gitHubOAuthService.authenticate(
            request,
            servletRequest.getHeader("User-Agent"),
            servletRequest.getHeader("X-Device-Id")
        );
    }

    @GetMapping("/admin/audit/stats")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "NFR audit statistics for SUPER_ADMIN")
    public AuthAuditStatsResponse getAuditStats(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null || principal.role() != Role.SUPER_ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("SUPER_ADMIN only");
        }
        Instant now           = Instant.now();
        Instant sevenDaysAgo  = now.minus(7,  ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        long active   = userRepository.countActiveUsers();
        long deleted  = userRepository.countDeletedUsers();
        long reg7d    = userRepository.countRegisteredSince(sevenDaysAgo);
        long reg30d   = userRepository.countRegisteredSince(thirtyDaysAgo);
        long mfa      = userRepository.countMfaEnabledUsers();
        long social   = userRepository.countSocialAuthUsers();
        long verified = userRepository.countVerifiedEmailUsers();
        java.util.Map<String, Long> byRole = userRepository.countUsersByRole();

        return new AuthAuditStatsResponse(active, active, deleted, byRole, reg7d, reg30d, mfa, social, verified);
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
