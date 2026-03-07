// src/main/java/com/edutech/parent/domain/port/out/FeePaymentRepository.java
package com.edutech.parent.domain.port.out;

import com.edutech.parent.domain.model.FeePayment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeePaymentRepository {
    FeePayment save(FeePayment payment);
    Optional<FeePayment> findById(UUID id);
    List<FeePayment> findByParentId(UUID parentId);
    List<FeePayment> findByParentIdAndStudentId(UUID parentId, UUID studentId);
}
