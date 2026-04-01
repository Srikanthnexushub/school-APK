package com.edutech.chat.infrastructure.adapter.in.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionSummaryDto(
    UUID id,
    String title,
    String lastMessage,
    int messageCount,
    Instant lastActiveAt
) {}
