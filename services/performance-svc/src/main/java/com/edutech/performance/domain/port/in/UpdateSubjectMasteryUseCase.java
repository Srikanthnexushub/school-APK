package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.SubjectMasteryResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateSubjectMasteryUseCase {

    SubjectMasteryResponse updateMastery(UUID studentId, UUID enrollmentId, String subject, BigDecimal newMasteryPercent);
}
