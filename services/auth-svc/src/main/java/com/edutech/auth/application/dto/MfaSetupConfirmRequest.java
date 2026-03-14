package com.edutech.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaSetupConfirmRequest(
    @NotBlank(message = "Authenticator code is required")
    @Pattern(regexp = "\\d{6}", message = "Authenticator code must be exactly 6 digits")
    String totpCode
) {}
