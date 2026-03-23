package com.edutech.center.application.dto;

import com.edutech.center.domain.model.ContentType;
import com.edutech.center.domain.model.Difficulty;
import com.edutech.center.domain.model.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Sent by the client after successfully uploading a file directly to MinIO.
 * objectKey is the key returned by the presigned-upload endpoint.
 */
public record ConfirmUploadRequest(
        UUID batchId,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 2000) String description,
        @NotNull ContentType type,
        @NotBlank String objectKey,
        Long fileSizeBytes,
        String mimeType,
        Integer pageCount,
        String thumbnailUrl,
        // optional metadata — AI will fill these in if omitted
        String subject,
        String board,
        String classGrade,
        Short yearOfPaper,
        ExamType examType,
        Difficulty difficulty,
        String language,
        List<String> tags
) {}
