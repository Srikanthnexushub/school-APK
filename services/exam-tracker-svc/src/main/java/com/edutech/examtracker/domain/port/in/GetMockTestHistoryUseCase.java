package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.MockTestAttemptResponse;

import java.util.List;
import java.util.UUID;

public interface GetMockTestHistoryUseCase {

    List<MockTestAttemptResponse> getMockHistory(UUID studentId, UUID enrollmentId);

    List<MockTestAttemptResponse> getMockHistoryByStudent(UUID studentId);
}
