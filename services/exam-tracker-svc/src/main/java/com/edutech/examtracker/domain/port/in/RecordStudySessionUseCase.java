package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.RecordStudySessionRequest;
import com.edutech.examtracker.application.dto.StudySessionResponse;

import java.util.UUID;

public interface RecordStudySessionUseCase {

    StudySessionResponse recordSession(UUID studentId, RecordStudySessionRequest request);
}
