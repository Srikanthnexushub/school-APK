package com.edutech.curricular.application.dto;

import java.util.UUID;

public record BoardResponse(UUID id, String boardCode, String boardName, String countryCode) {}
