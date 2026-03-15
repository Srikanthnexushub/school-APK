// src/main/java/com/edutech/center/application/dto/BulkImportConfirmResponse.java
package com.edutech.center.application.dto;

public record BulkImportConfirmResponse(
    int imported,
    int skipped,
    String message
) {}
