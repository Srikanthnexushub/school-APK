// src/main/java/com/edutech/parent/application/dto/StudentLinkResponse.java
package com.edutech.parent.application.dto;

import com.edutech.parent.domain.model.LinkStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentLinkResponse(
        UUID id,
        UUID parentId,
        UUID studentId,
        String studentName,
        UUID centerId,
        LinkStatus status,
        LocalDate dateOfBirth,
        String schoolName,
        String standard,
        String board,
        String rollNumber,
        Instant createdAt
) {}
