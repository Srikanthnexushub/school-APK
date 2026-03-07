package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.StudySessionResponse;

import java.util.List;
import java.util.UUID;

public interface GetStudySessionsUseCase {

    List<StudySessionResponse> getStudySessions(UUID studentId, UUID enrollmentId);
}
