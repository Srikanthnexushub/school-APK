package com.edutech.mentorsvc.infrastructure.persistence;

import com.edutech.mentorsvc.domain.model.MentorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataMentorProfileRepository extends JpaRepository<MentorProfile, UUID> {

    Optional<MentorProfile> findByEmail(String email);

    Optional<MentorProfile> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    @Query("SELECT m FROM MentorProfile m WHERE m.isAvailable = true AND m.deletedAt IS NULL")
    List<MentorProfile> findAllAvailableAndNotDeleted();
}
