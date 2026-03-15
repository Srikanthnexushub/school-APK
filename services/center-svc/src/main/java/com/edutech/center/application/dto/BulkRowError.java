// src/main/java/com/edutech/center/application/dto/BulkRowError.java
package com.edutech.center.application.dto;

public record BulkRowError(
    int row,
    String email,
    String field,
    String message,
    String suggestion
) {
    public BulkRowError(int row, String email, String field, String message) {
        this(row, email, field, message, null);
    }
}
