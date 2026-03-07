package com.edutech.careeroracle.infrastructure.persistence;

import com.edutech.careeroracle.domain.model.CareerProfile;
import com.edutech.careeroracle.domain.port.out.CareerProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CareerProfilePersistenceAdapter implements CareerProfileRepository {

    private final SpringDataCareerProfileRepository springDataRepository;

    public CareerProfilePersistenceAdapter(SpringDataCareerProfileRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CareerProfile save(CareerProfile careerProfile) {
        return springDataRepository.save(careerProfile);
    }

    @Override
    public Optional<CareerProfile> findById(UUID id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<CareerProfile> findByStudentId(UUID studentId) {
        return springDataRepository.findByStudentId(studentId);
    }

    @Override
    public boolean existsByStudentId(UUID studentId) {
        return springDataRepository.existsByStudentId(studentId);
    }
}
