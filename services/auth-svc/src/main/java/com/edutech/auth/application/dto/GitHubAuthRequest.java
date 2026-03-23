package com.edutech.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload sent by the frontend after GitHub OAuth callback.
 * The frontend posts the authorization code received from GitHub;
 * the server exchanges it for an access token and fetches user info.
 */
public record GitHubAuthRequest(
    @NotBlank String code,
    /** Optional — role the user wants to register as (STUDENT / PARENT / TEACHER). */
    String role
) {}
