// src/main/java/com/edutech/center/application/dto/BatchMemberResponse.java
package com.edutech.center.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BatchMemberResponse(
    UUID id,
    UUID batchId,
    UUID centerId,
    UUID studentId,
    String studentName,
    Instant enrolledAt,
    Instant withdrawnAt,
    boolean active
) {}
