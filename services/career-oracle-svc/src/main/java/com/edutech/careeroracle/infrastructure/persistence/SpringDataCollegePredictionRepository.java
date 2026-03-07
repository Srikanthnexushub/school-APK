package com.edutech.careeroracle.infrastructure.persistence;

import com.edutech.careeroracle.domain.model.CollegePrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataCollegePredictionRepository extends JpaRepository<CollegePrediction, UUID> {

    List<CollegePrediction> findByStudentId(UUID studentId);
}
