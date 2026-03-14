package com.edutech.auth.application.dto;

public record MfaSetupResponse(
    String secret,
    String qrCodeUri
) {}
