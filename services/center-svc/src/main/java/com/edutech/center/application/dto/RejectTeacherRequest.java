// src/main/java/com/edutech/center/application/dto/RejectTeacherRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTeacherRequest(
    @NotBlank @Size(max = 500) String reason
) {}
