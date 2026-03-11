// src/main/java/com/edutech/parent/application/dto/UpdateChildDetailsRequest.java
package com.edutech.parent.application.dto;

import java.time.LocalDate;

public record UpdateChildDetailsRequest(
        LocalDate dateOfBirth,
        String schoolName,
        String standard,
        String board,
        String rollNumber
) {}
