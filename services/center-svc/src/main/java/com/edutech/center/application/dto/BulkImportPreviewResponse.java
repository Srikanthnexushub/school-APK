// src/main/java/com/edutech/center/application/dto/BulkImportPreviewResponse.java
package com.edutech.center.application.dto;

import java.util.List;

public record BulkImportPreviewResponse(
    int totalRows,
    int validRows,
    int errorRows,
    List<BulkRowError> errors
) {}
