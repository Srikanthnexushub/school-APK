package com.edutech.chat.infrastructure.adapter.in.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ChatMessageDto(
    UUID id,
    String role,
    String content,
    String messageType,
    Map<String, Object> actionPayload,
    Instant createdAt
) {}
