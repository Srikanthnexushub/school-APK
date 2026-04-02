package com.edutech.curricular.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicYearRequest(UUID boardId, UUID gradeId, String yearLabel, LocalDate startDate, LocalDate endDate) {}
