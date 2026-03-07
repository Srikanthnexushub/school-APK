// src/main/java/com/edutech/parent/infrastructure/persistence/FeePaymentPersistenceAdapter.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.FeePayment;
import com.edutech.parent.domain.port.out.FeePaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class FeePaymentPersistenceAdapter implements FeePaymentRepository {

    private final SpringDataFeePaymentRepository repository;

    FeePaymentPersistenceAdapter(SpringDataFeePaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public FeePayment save(FeePayment payment) {
        return repository.save(payment);
    }

    @Override
    public Optional<FeePayment> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<FeePayment> findByParentId(UUID parentId) {
        return repository.findByParentId(parentId);
    }

    @Override
    public List<FeePayment> findByParentIdAndStudentId(UUID parentId, UUID studentId) {
        return repository.findByParentIdAndStudentId(parentId, studentId);
    }
}
