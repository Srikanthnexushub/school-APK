package com.edutech.psych.domain.port.out;

import com.edutech.psych.domain.model.PsychProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PsychProfileRepository {

    Optional<PsychProfile> findById(UUID id);

    Optional<PsychProfile> findByStudentIdAndCenterId(UUID studentId, UUID centerId);

    List<PsychProfile> findByCenterId(UUID centerId);

    List<PsychProfile> findByStudentId(UUID studentId);

    PsychProfile save(PsychProfile profile);
}
