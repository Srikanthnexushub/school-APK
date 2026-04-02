package com.edutech.curricular.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ActivitySessionResponse(UUID id, UUID activityId, LocalDate sessionDate, String venue, int durationMins, String notes) {}
