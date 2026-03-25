// src/main/java/com/edutech/center/domain/port/out/BatchMemberRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.BatchMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchMemberRepository {
    BatchMember save(BatchMember member);
    Optional<BatchMember> findByBatchIdAndStudentId(UUID batchId, UUID studentId);
    List<BatchMember> findActiveByBatchId(UUID batchId);
    List<BatchMember> findByStudentId(UUID studentId);
}
