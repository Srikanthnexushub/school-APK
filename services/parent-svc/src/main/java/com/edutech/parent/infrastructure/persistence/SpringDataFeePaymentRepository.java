// src/main/java/com/edutech/parent/infrastructure/persistence/SpringDataFeePaymentRepository.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataFeePaymentRepository extends JpaRepository<FeePayment, UUID> {

    @Query("SELECT p FROM FeePayment p WHERE p.parentId = :parentId ORDER BY p.paymentDate DESC")
    List<FeePayment> findByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT p FROM FeePayment p WHERE p.parentId = :parentId AND p.studentId = :studentId ORDER BY p.paymentDate DESC")
    List<FeePayment> findByParentIdAndStudentId(@Param("parentId") UUID parentId,
                                                @Param("studentId") UUID studentId);
}
