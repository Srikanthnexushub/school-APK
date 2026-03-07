// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataCenterRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.CoachingCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataCenterRepository extends JpaRepository<CoachingCenter, UUID> {

    @Query("SELECT c FROM CoachingCenter c WHERE c.deletedAt IS NULL")
    List<CoachingCenter> findAllActive();

    @Query("SELECT c FROM CoachingCenter c WHERE c.ownerId = :ownerId AND c.deletedAt IS NULL")
    List<CoachingCenter> findByOwnerIdActive(UUID ownerId);

    @Query("SELECT c FROM CoachingCenter c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CoachingCenter> findByIdActive(UUID id);

    boolean existsByCode(String code);
}
