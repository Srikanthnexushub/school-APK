package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.time.Instant;
import java.util.UUID;

public record ExamItem(
    UUID examId,
    String title,
    double scoredMarks,
    double totalMarks,
    double percentage,
    String letterGrade,
    Instant submittedAt
) {}
