package com.edutech.auth.api;

import com.edutech.auth.application.dto.AuthPrincipal;
import com.edutech.auth.application.dto.MfaSetupConfirmRequest;
import com.edutech.auth.application.dto.MfaSetupResponse;
import com.edutech.auth.application.dto.MfaStatusResponse;
import com.edutech.auth.application.dto.MfaVerifyRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.MfaUseCase;
import com.edutech.auth.domain.port.out.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@Tag(name = "MFA", description = "TOTP two-factor authentication setup and verification")
public class MfaController {

    private final MfaUseCase mfaUseCase;
    private final UserRepository userRepository;

    public MfaController(MfaUseCase mfaUseCase, UserRepository userRepository) {
        this.mfaUseCase = mfaUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get MFA enabled status for the current user")
    public MfaStatusResponse status(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = userRepository.findById(principal.userId()).orElseThrow();
        return new MfaStatusResponse(user.isMfaEnabled());
    }

    @PostMapping("/setup")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Initiate MFA setup — returns TOTP secret and QR code URI")
    public MfaSetupResponse setup(@AuthenticationPrincipal AuthPrincipal principal) {
        return mfaUseCase.initSetup(principal.userId());
    }

    @PostMapping("/setup/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Confirm MFA setup by verifying first TOTP code — enables MFA on the account")
    public void confirmSetup(@AuthenticationPrincipal AuthPrincipal principal,
                             @Valid @RequestBody MfaSetupConfirmRequest request) {
        mfaUseCase.confirmSetup(principal.userId(), request.totpCode());
    }

    @DeleteMapping("/setup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Disable MFA — requires a valid TOTP code to confirm")
    public void disable(@AuthenticationPrincipal AuthPrincipal principal,
                        @Valid @RequestBody MfaSetupConfirmRequest request) {
        mfaUseCase.disable(principal.userId(), request.totpCode());
    }

    @PostMapping("/verify")
    @Operation(summary = "Complete MFA login — verifies TOTP code and returns full token pair")
    public TokenPair verify(@Valid @RequestBody MfaVerifyRequest request) {
        return mfaUseCase.verifyLogin(
            request.pendingMfaToken(),
            request.totpCode(),
            request.deviceFingerprint()
        );
    }
}
