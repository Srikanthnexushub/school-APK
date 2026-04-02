package com.edutech.curricular.application.dto;

public record ActivityCreateRequest(String name, String description, String category, int maxParticipants, String scheduleDescription) {}
