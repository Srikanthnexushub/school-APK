package com.edutech.curricular.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CoverageLogRequest(UUID topicId, LocalDate taughtOn, String deliveryMethod, int durationMins, String notes) {}
