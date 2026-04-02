package com.edutech.center.application.dto;

import com.edutech.center.domain.model.ContentStatus;
import com.edutech.center.domain.model.ContentType;
import com.edutech.center.domain.model.Difficulty;
import com.edutech.center.domain.model.ExamType;

import java.time.Instant;
import java.util.UUID;

public record ContentItemResponse(
        UUID id,
        UUID centerId,
        UUID batchId,
        String title,
        String description,
        ContentType type,
        String fileUrl,
        Long fileSizeBytes,
        UUID uploadedByUserId,
        ContentStatus status,
        // metadata
        String subject,
        String board,
        String classGrade,
        Short yearOfPaper,
        ExamType examType,
        Difficulty difficulty,
        String language,
        String[] tags,
        long downloadCount,
        String aiSummary,
        String thumbnailUrl,
        String mimeType,
        Integer pageCount,
        Instant createdAt,
        Instant updatedAt,
        boolean platformResource,
        String stream
) {}
