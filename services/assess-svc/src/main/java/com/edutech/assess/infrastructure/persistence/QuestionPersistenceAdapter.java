// src/main/java/com/edutech/assess/infrastructure/persistence/QuestionPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.port.out.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class QuestionPersistenceAdapter implements QuestionRepository {

    private final SpringDataQuestionRepository repository;

    QuestionPersistenceAdapter(SpringDataQuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Question save(Question q) {
        return repository.save(q);
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return repository.findByIdActive(id);
    }

    @Override
    public List<Question> findByExamId(UUID examId) {
        return repository.findByExamId(examId);
    }

    @Override
    public List<Question> saveAll(List<Question> questions) {
        return repository.saveAll(questions);
    }

    @Override
    public int countByExamId(UUID examId) {
        return repository.countByExamId(examId);
    }
}
