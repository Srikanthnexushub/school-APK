// src/main/java/com/edutech/center/application/dto/AddBatchMemberRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddBatchMemberRequest(
    @NotNull UUID studentId,
    @NotBlank @Size(max = 200) String studentName
) {}
