package com.edutech.careeroracle.infrastructure.persistence;

import com.edutech.careeroracle.domain.model.CareerRecommendation;
import com.edutech.careeroracle.domain.port.out.CareerRecommendationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CareerRecommendationPersistenceAdapter implements CareerRecommendationRepository {

    private final SpringDataCareerRecommendationRepository springDataRepository;

    public CareerRecommendationPersistenceAdapter(SpringDataCareerRecommendationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CareerRecommendation save(CareerRecommendation recommendation) {
        return springDataRepository.save(recommendation);
    }

    @Override
    public List<CareerRecommendation> saveAll(List<CareerRecommendation> recommendations) {
        return springDataRepository.saveAll(recommendations);
    }

    @Override
    public List<CareerRecommendation> findActiveByStudentId(UUID studentId) {
        return springDataRepository.findActiveByStudentId(studentId);
    }

    @Override
    public void deactivateAllByStudentId(UUID studentId) {
        springDataRepository.deactivateAllByStudentId(studentId);
    }
}
