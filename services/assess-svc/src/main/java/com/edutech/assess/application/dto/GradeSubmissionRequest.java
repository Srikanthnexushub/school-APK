// src/main/java/com/edutech/assess/application/dto/GradeSubmissionRequest.java
package com.edutech.assess.application.dto;

import jakarta.validation.constraints.DecimalMin;

public record GradeSubmissionRequest(
        @DecimalMin("0.0") double score,
        String feedback
) {}
