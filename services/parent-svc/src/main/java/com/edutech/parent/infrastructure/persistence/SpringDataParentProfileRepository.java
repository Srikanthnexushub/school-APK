// src/main/java/com/edutech/parent/infrastructure/persistence/SpringDataParentProfileRepository.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.ParentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataParentProfileRepository extends JpaRepository<ParentProfile, UUID> {

    @Query("SELECT p FROM ParentProfile p WHERE p.userId = :userId AND p.deletedAt IS NULL")
    Optional<ParentProfile> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(p) > 0 FROM ParentProfile p WHERE p.userId = :userId AND p.deletedAt IS NULL")
    boolean existsByUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM ParentProfile p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<ParentProfile> findByIdActive(@Param("id") UUID id);

    @Query("SELECT p FROM ParentProfile p WHERE p.email = :email AND p.deletedAt IS NULL")
    Optional<ParentProfile> findByEmailActive(@Param("email") String email);
}
