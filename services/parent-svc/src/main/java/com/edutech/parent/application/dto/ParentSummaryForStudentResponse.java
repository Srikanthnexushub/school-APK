// src/main/java/com/edutech/parent/application/dto/ParentSummaryForStudentResponse.java
package com.edutech.parent.application.dto;

import java.util.UUID;

/**
 * Compact parent profile returned to a student who calls GET /api/v1/parents/by-student.
 * Phone is masked server-side (e.g. ****1234) to protect PII.
 */
public record ParentSummaryForStudentResponse(
        UUID parentProfileId,
        String name,
        String maskedPhone,
        String email,
        String relationship,
        boolean verified
) {}
