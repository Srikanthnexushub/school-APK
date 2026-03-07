package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataRecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findAllByStudentIdAndAcknowledgedFalse(UUID studentId);
}
