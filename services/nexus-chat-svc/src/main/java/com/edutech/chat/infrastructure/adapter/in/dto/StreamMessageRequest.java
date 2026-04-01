package com.edutech.chat.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StreamMessageRequest(
    @NotBlank @Size(max = 4000) String message,
    @Size(max = 100) String pageContext
) {}
