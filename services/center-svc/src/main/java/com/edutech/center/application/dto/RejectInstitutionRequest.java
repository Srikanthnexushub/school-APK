// src/main/java/com/edutech/center/application/dto/RejectInstitutionRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectInstitutionRequest(
    @NotBlank @Size(max = 500) String reason
) {}
