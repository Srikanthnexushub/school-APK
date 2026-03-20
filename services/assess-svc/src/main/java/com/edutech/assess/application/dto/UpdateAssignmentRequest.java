// src/main/java/com/edutech/assess/application/dto/UpdateAssignmentRequest.java
package com.edutech.assess.application.dto;

import java.time.Instant;

public record UpdateAssignmentRequest(
        String title,
        String description,
        String instructions,
        String attachmentUrl,
        Instant dueDate,
        double totalMarks,
        double passingMarks
) {}
