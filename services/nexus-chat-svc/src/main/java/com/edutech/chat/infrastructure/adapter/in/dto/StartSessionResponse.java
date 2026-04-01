package com.edutech.chat.infrastructure.adapter.in.dto;

import java.util.UUID;

public record StartSessionResponse(
    UUID sessionId,
    String greeting,
    String firstName
) {}
