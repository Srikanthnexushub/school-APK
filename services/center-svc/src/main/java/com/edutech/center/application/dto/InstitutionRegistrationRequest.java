// src/main/java/com/edutech/center/application/dto/InstitutionRegistrationRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstitutionRegistrationRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 50) String institutionType,
    @Size(max = 100) String board,
    @NotBlank @Size(max = 500) String address,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Size(max = 10) @Pattern(regexp = "^\\d{6}$", message = "pincode must be 6 digits") String pincode,
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 500) String website
) {}
