package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.PsychProfile;
import com.edutech.psych.domain.port.out.PsychProfileRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PsychProfilePersistenceAdapter implements PsychProfileRepository {

    private final SpringDataPsychProfileRepository springData;

    public PsychProfilePersistenceAdapter(SpringDataPsychProfileRepository springData) {
        this.springData = springData;
    }

    @Override
    public PsychProfile save(PsychProfile profile) {
        return springData.save(profile);
    }

    @Override
    public Optional<PsychProfile> findById(UUID id) {
        return springData.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<PsychProfile> findByStudentIdAndCenterId(UUID studentId, UUID centerId) {
        return springData.findByStudentIdAndCenterIdAndDeletedAtIsNull(studentId, centerId);
    }

    @Override
    public List<PsychProfile> findByCenterId(UUID centerId) {
        return springData.findByCenterIdAndDeletedAtIsNull(centerId);
    }

    @Override
    public List<PsychProfile> findByStudentId(UUID studentId) {
        return springData.findByStudentIdAndDeletedAtIsNull(studentId);
    }

}
