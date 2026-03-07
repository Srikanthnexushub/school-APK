package com.edutech.careeroracle.infrastructure.persistence;

import com.edutech.careeroracle.domain.model.CareerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataCareerProfileRepository extends JpaRepository<CareerProfile, UUID> {

    Optional<CareerProfile> findByStudentId(UUID studentId);

    boolean existsByStudentId(UUID studentId);
}
