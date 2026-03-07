package com.edutech.psych.domain.port.out;

import com.edutech.psych.domain.model.CareerMapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareerMappingRepository {

    Optional<CareerMapping> findById(UUID id);

    List<CareerMapping> findByProfileId(UUID profileId);

    CareerMapping save(CareerMapping mapping);
}
