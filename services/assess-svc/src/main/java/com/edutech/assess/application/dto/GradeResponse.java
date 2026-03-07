// src/main/java/com/edutech/assess/application/dto/GradeResponse.java
package com.edutech.assess.application.dto;

import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
        UUID id,
        UUID submissionId,
        UUID examId,
        UUID studentId,
        UUID batchId,
        UUID centerId,
        double percentage,
        String letterGrade,
        boolean passed,
        Instant createdAt
) {}
