package com.edutech.aimentor.application.dto;

import com.edutech.aimentor.domain.model.DoubtStatus;
import com.edutech.aimentor.domain.model.SubjectArea;

import java.time.Instant;
import java.util.UUID;

public record DoubtTicketResponse(
        UUID id,
        UUID studentId,
        UUID enrollmentId,
        SubjectArea subjectArea,
        String question,
        String aiAnswer,
        DoubtStatus status,
        Instant resolvedAt,
        Instant createdAt
) {}
