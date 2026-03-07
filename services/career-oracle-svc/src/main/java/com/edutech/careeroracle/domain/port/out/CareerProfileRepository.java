package com.edutech.careeroracle.domain.port.out;

import com.edutech.careeroracle.domain.model.CareerProfile;

import java.util.Optional;
import java.util.UUID;

public interface CareerProfileRepository {

    CareerProfile save(CareerProfile careerProfile);

    Optional<CareerProfile> findById(UUID id);

    Optional<CareerProfile> findByStudentId(UUID studentId);

    boolean existsByStudentId(UUID studentId);
}
