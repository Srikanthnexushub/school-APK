package com.edutech.curricular.application.dto;

import java.util.UUID;

public record SubjectResponse(UUID id, UUID boardId, String subjectCode, String subjectName) {}
