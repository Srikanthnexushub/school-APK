package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.util.List;
import java.util.UUID;

public record StudentProfileDto(
    UUID userId,
    String fullName,
    String currentClass,
    String board,
    String stream,
    List<String> subjects,
    int targetYear
) {
    public static StudentProfileDto empty(UUID userId) {
        return new StudentProfileDto(userId, "Student", "Unknown", "CBSE", null, List.of(), 2026);
    }
}
