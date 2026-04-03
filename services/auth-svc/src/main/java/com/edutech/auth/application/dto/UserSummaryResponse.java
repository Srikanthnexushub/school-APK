package com.edutech.auth.application.dto;

import java.util.UUID;

public record UserSummaryResponse(UUID userId, String firstName, String lastName, String email) {}
