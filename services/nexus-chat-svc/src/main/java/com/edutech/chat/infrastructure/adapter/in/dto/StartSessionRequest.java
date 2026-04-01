package com.edutech.chat.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.Size;

public record StartSessionRequest(
    @Size(max = 100) String pageContext
) {}
