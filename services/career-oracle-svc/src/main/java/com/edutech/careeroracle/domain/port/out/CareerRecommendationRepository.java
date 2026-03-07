package com.edutech.careeroracle.domain.port.out;

import com.edutech.careeroracle.domain.model.CareerRecommendation;

import java.util.List;
import java.util.UUID;

public interface CareerRecommendationRepository {

    CareerRecommendation save(CareerRecommendation recommendation);

    List<CareerRecommendation> saveAll(List<CareerRecommendation> recommendations);

    List<CareerRecommendation> findActiveByStudentId(UUID studentId);

    void deactivateAllByStudentId(UUID studentId);
}
