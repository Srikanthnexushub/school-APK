package com.edutech.curricular.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CoverageLogResponse(UUID id, UUID topicId, String topicTitle, LocalDate taughtOn, String deliveryMethod, int durationMins) {}
