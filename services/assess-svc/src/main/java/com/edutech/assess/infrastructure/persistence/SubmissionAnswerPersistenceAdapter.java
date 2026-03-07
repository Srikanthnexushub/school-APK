// src/main/java/com/edutech/assess/infrastructure/persistence/SubmissionAnswerPersistenceAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.SubmissionAnswer;
import com.edutech.assess.domain.port.out.SubmissionAnswerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class SubmissionAnswerPersistenceAdapter implements SubmissionAnswerRepository {

    private final SpringDataSubmissionAnswerRepository repository;

    SubmissionAnswerPersistenceAdapter(SpringDataSubmissionAnswerRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubmissionAnswer save(SubmissionAnswer a) {
        return repository.save(a);
    }

    @Override
    public List<SubmissionAnswer> saveAll(List<SubmissionAnswer> answers) {
        return repository.saveAll(answers);
    }

    @Override
    public List<SubmissionAnswer> findBySubmissionId(UUID submissionId) {
        return repository.findBySubmissionId(submissionId);
    }
}
