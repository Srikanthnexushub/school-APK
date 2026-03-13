// src/main/java/com/edutech/parent/application/dto/LinkStudentRequest.java
package com.edutech.parent.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record LinkStudentRequest(
        @NotNull UUID studentId,
        @NotBlank String studentName,
        @NotNull UUID centerId,
        String relationship,
        LocalDate dateOfBirth,
        String schoolName,
        String standard,
        String board,
        String rollNumber
) {}
