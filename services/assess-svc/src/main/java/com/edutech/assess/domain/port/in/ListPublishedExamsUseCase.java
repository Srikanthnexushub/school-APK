// src/main/java/com/edutech/assess/domain/port/in/ListPublishedExamsUseCase.java
package com.edutech.assess.domain.port.in;

import com.edutech.assess.application.dto.StudentExamResponse;

import java.util.List;
import java.util.UUID;

public interface ListPublishedExamsUseCase {
    List<StudentExamResponse> listPublishedExams(UUID studentId);
}
