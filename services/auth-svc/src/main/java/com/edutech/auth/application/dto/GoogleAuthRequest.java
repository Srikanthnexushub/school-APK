package com.edutech.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload sent by the frontend after the user completes Google Sign-In.
 * The frontend (via @react-oauth/google) obtains a Google ID token and
 * posts it here for server-side verification.
 */
public record GoogleAuthRequest(
    @NotBlank String idToken,
    /** Optional — role the user wants to register as (STUDENT / PARENT / TEACHER). */
    String role
) {}
