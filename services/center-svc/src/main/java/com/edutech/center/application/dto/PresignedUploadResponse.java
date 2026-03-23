package com.edutech.center.application.dto;

public record PresignedUploadResponse(
        String uploadUrl,
        String objectKey,
        int expirySeconds
) {}
