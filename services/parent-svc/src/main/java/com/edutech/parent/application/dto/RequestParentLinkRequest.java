package com.edutech.parent.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestParentLinkRequest(
        @NotBlank @Email String parentEmail
) {}
