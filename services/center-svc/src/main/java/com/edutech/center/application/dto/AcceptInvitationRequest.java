// src/main/java/com/edutech/center/application/dto/AcceptInvitationRequest.java
package com.edutech.center.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptInvitationRequest(
    @NotBlank String token,
    @NotNull UUID userId
) {}
