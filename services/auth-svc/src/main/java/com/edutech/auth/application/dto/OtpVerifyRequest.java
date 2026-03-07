// src/main/java/com/edutech/auth/application/dto/OtpVerifyRequest.java
package com.edutech.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 6) String otp,
    @NotBlank String purpose
) {}
