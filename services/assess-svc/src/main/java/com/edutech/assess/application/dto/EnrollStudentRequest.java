// src/main/java/com/edutech/assess/application/dto/EnrollStudentRequest.java
package com.edutech.assess.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnrollStudentRequest(@NotNull UUID studentId) {}
