package com.edutech.chat.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RecentExamSummary(
    UUID examId,
    String title,
    double scoredMarks,
    double totalMarks,
    double percentage,
    String letterGrade,
    Instant submittedAt
) {}
