package com.edutech.chat.infrastructure.adapter.in.dto;

import java.util.UUID;

public record SendMessageResponse(
    UUID messageId,
    String content,
    String actionJson
) {}
