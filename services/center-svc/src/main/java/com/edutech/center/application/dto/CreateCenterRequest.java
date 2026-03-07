// src/main/java/com/edutech/center/application/dto/CreateCenterRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCenterRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9]+$",
        message = "code must be uppercase alphanumeric") String code,
    @NotBlank @Size(max = 500) String address,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Size(max = 10) String pincode,
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 500) String website,
    @Size(max = 1000) String logoUrl,
    UUID ownerId
) {}
