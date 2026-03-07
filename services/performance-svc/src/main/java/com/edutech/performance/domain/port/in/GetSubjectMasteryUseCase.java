package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.SubjectMasteryResponse;

import java.util.List;
import java.util.UUID;

public interface GetSubjectMasteryUseCase {

    List<SubjectMasteryResponse> getSubjectMastery(UUID studentId, UUID enrollmentId);
}
