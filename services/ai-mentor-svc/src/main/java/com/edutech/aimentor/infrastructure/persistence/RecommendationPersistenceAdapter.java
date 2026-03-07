package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.Recommendation;
import com.edutech.aimentor.domain.port.out.RecommendationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RecommendationPersistenceAdapter implements RecommendationRepository {

    private final SpringDataRecommendationRepository recommendationRepo;

    public RecommendationPersistenceAdapter(SpringDataRecommendationRepository recommendationRepo) {
        this.recommendationRepo = recommendationRepo;
    }

    @Override
    public Recommendation save(Recommendation recommendation) {
        return recommendationRepo.save(recommendation);
    }

    @Override
    public List<Recommendation> findActiveByStudentId(UUID studentId) {
        return recommendationRepo.findAllByStudentIdAndAcknowledgedFalse(studentId);
    }

    @Override
    public Optional<Recommendation> findById(UUID id) {
        return recommendationRepo.findById(id);
    }
}
