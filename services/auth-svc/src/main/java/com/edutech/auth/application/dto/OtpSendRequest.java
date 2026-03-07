// src/main/java/com/edutech/auth/application/dto/OtpSendRequest.java
package com.edutech.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpSendRequest(
    @NotBlank @Email String email,
    @NotBlank String purpose,
    @NotBlank @Pattern(regexp = "email|sms") String channel
) {}
