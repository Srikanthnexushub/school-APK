// src/main/java/com/edutech/parent/application/dto/CreateParentProfileRequest.java
package com.edutech.parent.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateParentProfileRequest(
        @NotBlank String name,
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number") String phone
) {}
