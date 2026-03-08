package com.edutech.parent.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationResponse(
        Long id,
        String parentId,
        String studentId,
        String title,
        String status,
        List<MessageResponse> messages,
        LocalDateTime createdAt
) {}
