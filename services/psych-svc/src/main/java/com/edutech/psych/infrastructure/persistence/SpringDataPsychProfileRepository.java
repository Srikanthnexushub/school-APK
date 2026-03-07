package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.PsychProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataPsychProfileRepository extends JpaRepository<PsychProfile, UUID> {

    Optional<PsychProfile> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PsychProfile> findByStudentIdAndCenterIdAndDeletedAtIsNull(UUID studentId, UUID centerId);

    List<PsychProfile> findByCenterIdAndDeletedAtIsNull(UUID centerId);
}
