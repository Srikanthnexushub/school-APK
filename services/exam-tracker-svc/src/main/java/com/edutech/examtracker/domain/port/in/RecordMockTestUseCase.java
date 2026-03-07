package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.MockTestAttemptResponse;
import com.edutech.examtracker.application.dto.RecordMockTestRequest;

import java.util.UUID;

public interface RecordMockTestUseCase {

    MockTestAttemptResponse recordMockTest(UUID studentId, RecordMockTestRequest request);
}
