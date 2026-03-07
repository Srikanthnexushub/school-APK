// src/main/java/com/edutech/assess/domain/port/out/QuestionRepository.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    Optional<Question> findById(UUID id);
    List<Question> findByExamId(UUID examId);
    List<Question> saveAll(List<Question> questions);
}
