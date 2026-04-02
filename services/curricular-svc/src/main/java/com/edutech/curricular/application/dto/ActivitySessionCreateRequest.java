package com.edutech.curricular.application.dto;

import java.time.LocalDate;

public record ActivitySessionCreateRequest(LocalDate sessionDate, String venue, int durationMins, String notes) {}
