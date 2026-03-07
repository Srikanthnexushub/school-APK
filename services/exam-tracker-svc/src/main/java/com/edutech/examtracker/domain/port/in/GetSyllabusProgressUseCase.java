package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.SyllabusProgressResponse;

import java.util.UUID;

public interface GetSyllabusProgressUseCase {

    SyllabusProgressResponse getSyllabusProgress(UUID studentId, UUID enrollmentId);
}
