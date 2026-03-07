// src/main/java/com/edutech/assess/domain/port/out/SubmissionAnswerRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.SubmissionAnswer;

import java.util.List;
import java.util.UUID;

public interface SubmissionAnswerRepository {
    SubmissionAnswer save(SubmissionAnswer answer);
    List<SubmissionAnswer> saveAll(List<SubmissionAnswer> answers);
    List<SubmissionAnswer> findBySubmissionId(UUID submissionId);
}
