package com.edutech.aimentor.domain.port.out;

import com.edutech.aimentor.domain.model.Recommendation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepository {

    Recommendation save(Recommendation recommendation);

    List<Recommendation> findActiveByStudentId(UUID studentId);

    Optional<Recommendation> findById(UUID id);
}
