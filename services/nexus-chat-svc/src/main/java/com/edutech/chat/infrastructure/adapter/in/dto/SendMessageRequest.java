package com.edutech.chat.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
    @NotBlank String message,
    String pageContext
) {}
