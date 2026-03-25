// src/main/java/com/edutech/center/infrastructure/persistence/BatchMemberPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.BatchMember;
import com.edutech.center.domain.port.out.BatchMemberRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BatchMemberPersistenceAdapter implements BatchMemberRepository {

    private final SpringDataBatchMemberRepository jpa;

    public BatchMemberPersistenceAdapter(SpringDataBatchMemberRepository jpa) { this.jpa = jpa; }

    @Override public BatchMember save(BatchMember member) { return jpa.save(member); }
    @Override public Optional<BatchMember> findByBatchIdAndStudentId(UUID batchId, UUID studentId) { return jpa.findByBatchIdAndStudentId(batchId, studentId); }
    @Override public List<BatchMember> findActiveByBatchId(UUID batchId) { return jpa.findActiveByBatchId(batchId); }
    @Override public List<BatchMember> findByStudentId(UUID studentId) { return jpa.findByStudentId(studentId); }
}
