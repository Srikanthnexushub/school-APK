// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataBatchMemberRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.BatchMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataBatchMemberRepository extends JpaRepository<BatchMember, UUID> {

    @Query("SELECT m FROM BatchMember m WHERE m.batchId = :batchId AND m.withdrawnAt IS NULL")
    List<BatchMember> findActiveByBatchId(@Param("batchId") UUID batchId);

    Optional<BatchMember> findByBatchIdAndStudentId(UUID batchId, UUID studentId);

    List<BatchMember> findByStudentId(UUID studentId);
}
