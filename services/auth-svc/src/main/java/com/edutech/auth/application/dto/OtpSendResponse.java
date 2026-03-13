package com.edutech.auth.application.dto;

/**
 * Response returned by POST /api/v1/otp/send.
 * Lets the frontend display a "X resends remaining" counter.
 */
public record OtpSendResponse(
    int resendsRemaining,
    int maxResends
) {}
