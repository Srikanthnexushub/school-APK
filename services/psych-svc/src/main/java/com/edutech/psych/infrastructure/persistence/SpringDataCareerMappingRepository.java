package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.CareerMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataCareerMappingRepository extends JpaRepository<CareerMapping, UUID> {

    Optional<CareerMapping> findByIdAndDeletedAtIsNull(UUID id);

    List<CareerMapping> findByProfileIdAndDeletedAtIsNull(UUID profileId);
}
