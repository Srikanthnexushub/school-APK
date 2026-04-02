package com.edutech.curricular.application.dto;

import java.util.UUID;

public record ActivityResponse(UUID id, UUID centerId, String name, String description, String category, int maxParticipants, String scheduleDescription, boolean active) {}
