package com.edutech.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterChildRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
             message = "Password must contain at least 1 uppercase letter, 1 digit, and 1 special character")
    String password,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 20) String phoneNumber
) {}
