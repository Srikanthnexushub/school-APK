package com.edutech.curricular.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicYearResponse(UUID id, UUID centerId, String yearLabel, LocalDate startDate, LocalDate endDate, boolean active) {}
