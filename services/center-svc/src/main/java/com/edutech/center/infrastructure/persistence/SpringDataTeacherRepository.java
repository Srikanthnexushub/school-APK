// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataTeacherRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTeacherRepository extends JpaRepository<Teacher, UUID> {

    @Query("SELECT t FROM Teacher t WHERE t.centerId = :centerId AND t.deletedAt IS NULL")
    List<Teacher> findByCenterIdActive(UUID centerId);

    @Query("SELECT t FROM Teacher t WHERE t.id = :id AND t.centerId = :centerId AND t.deletedAt IS NULL")
    Optional<Teacher> findByIdAndCenterIdActive(UUID id, UUID centerId);

    @Query("SELECT COUNT(t) > 0 FROM Teacher t WHERE t.userId = :userId AND t.centerId = :centerId AND t.deletedAt IS NULL")
    boolean existsByUserIdAndCenterId(UUID userId, UUID centerId);

    @Query("SELECT t FROM Teacher t WHERE t.userId = :userId AND t.deletedAt IS NULL")
    List<Teacher> findByUserIdActive(UUID userId);
}
