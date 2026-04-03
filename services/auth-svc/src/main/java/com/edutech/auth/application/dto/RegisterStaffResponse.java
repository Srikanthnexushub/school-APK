// src/main/java/com/edutech/auth/application/dto/RegisterStaffResponse.java
package com.edutech.auth.application.dto;

import java.util.UUID;

public record RegisterStaffResponse(UUID userId, String email) {}
