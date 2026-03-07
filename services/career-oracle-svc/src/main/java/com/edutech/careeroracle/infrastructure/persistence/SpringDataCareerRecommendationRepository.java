package com.edutech.careeroracle.infrastructure.persistence;

import com.edutech.careeroracle.domain.model.CareerRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataCareerRecommendationRepository extends JpaRepository<CareerRecommendation, UUID> {

    @Query("SELECT r FROM CareerRecommendation r WHERE r.studentId = :studentId AND r.isActive = true ORDER BY r.rankOrder ASC")
    List<CareerRecommendation> findActiveByStudentId(@Param("studentId") UUID studentId);

    @Modifying
    @Query("UPDATE CareerRecommendation r SET r.isActive = false WHERE r.studentId = :studentId AND r.isActive = true")
    void deactivateAllByStudentId(@Param("studentId") UUID studentId);
}
