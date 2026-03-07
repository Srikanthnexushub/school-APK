// src/main/java/com/edutech/center/application/dto/UploadContentRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UploadContentRequest(
    UUID batchId,
    @NotBlank @Size(max = 500) String title,
    @Size(max = 2000) String description,
    @NotNull ContentType type,
    @NotBlank @Size(max = 2000) String fileUrl,
    @Min(0) Long fileSizeBytes
) {}
