// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataCenterRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.CenterStatus;
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

    @Query("SELECT c FROM CoachingCenter c WHERE c.code = :code AND c.deletedAt IS NULL")
    Optional<CoachingCenter> findByCodeActive(String code);

    @Query("SELECT c FROM CoachingCenter c WHERE c.status = :status AND c.deletedAt IS NULL")
    List<CoachingCenter> findByStatus(CenterStatus status);

    @Query("SELECT COUNT(c) > 0 FROM CoachingCenter c WHERE LOWER(c.name) = LOWER(:name) AND LOWER(c.city) = LOWER(:city) AND c.deletedAt IS NULL")
    boolean existsByNameAndCity(String name, String city);

    @Query(value = "SELECT COUNT(*) FROM center_schema.centers WHERE code LIKE :prefix%", nativeQuery = true)
    long countByCodePrefix(String prefix);

    @Query("SELECT c FROM CoachingCenter c WHERE c.adminUserId = :adminUserId")
    Optional<CoachingCenter> findByAdminUserId(UUID adminUserId);
}
