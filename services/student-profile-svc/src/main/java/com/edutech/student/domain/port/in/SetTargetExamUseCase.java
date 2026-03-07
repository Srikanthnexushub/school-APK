package com.edutech.student.domain.port.in;

import com.edutech.student.application.dto.SetTargetExamRequest;
import com.edutech.student.application.dto.TargetExamResponse;

import java.util.UUID;

public interface SetTargetExamUseCase {
    TargetExamResponse setTargetExam(UUID studentId, SetTargetExamRequest request);
}
