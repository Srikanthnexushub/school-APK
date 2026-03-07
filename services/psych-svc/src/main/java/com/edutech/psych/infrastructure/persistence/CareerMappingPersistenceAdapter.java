package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.CareerMapping;
import com.edutech.psych.domain.port.out.CareerMappingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CareerMappingPersistenceAdapter implements CareerMappingRepository {

    private final SpringDataCareerMappingRepository springData;

    public CareerMappingPersistenceAdapter(SpringDataCareerMappingRepository springData) {
        this.springData = springData;
    }

    @Override
    public CareerMapping save(CareerMapping careerMapping) {
        return springData.save(careerMapping);
    }

    @Override
    public Optional<CareerMapping> findById(UUID id) {
        return springData.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<CareerMapping> findByProfileId(UUID profileId) {
        return springData.findByProfileIdAndDeletedAtIsNull(profileId);
    }

}
