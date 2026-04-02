package com.edutech.center.application.dto;

import com.edutech.center.domain.model.ContentType;
import com.edutech.center.domain.model.Difficulty;
import com.edutech.center.domain.model.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request to register an external link (YouTube, web resource) as library content.
 * No file upload required — URL is stored as-is in fileUrl.
 * type must be LINK or VIDEO.
 */
public record RegisterLinkRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 2000) String description,
        @NotNull ContentType type,
        @NotBlank @Size(max = 2000) String externalUrl,
        String subject,
        String board,
        String classGrade,
        String stream,
        ExamType examType,
        Difficulty difficulty,
        String language,
        List<String> tags,
        String thumbnailUrl
) {}
