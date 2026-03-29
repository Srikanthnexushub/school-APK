// src/main/java/com/edutech/auth/application/dto/AssignUserCenterRequest.java
package com.edutech.auth.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignUserCenterRequest(@NotNull UUID centerId) {}
