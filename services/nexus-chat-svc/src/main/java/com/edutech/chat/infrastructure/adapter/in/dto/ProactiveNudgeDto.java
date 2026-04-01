package com.edutech.chat.infrastructure.adapter.in.dto;

import java.time.Instant;
import java.util.UUID;

public record ProactiveNudgeDto(
    UUID id,
    String message,
    String actionUrl,
    String triggerType,
    Instant createdAt
) {}
