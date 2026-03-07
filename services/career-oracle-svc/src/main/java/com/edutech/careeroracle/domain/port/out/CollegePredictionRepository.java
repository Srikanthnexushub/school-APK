package com.edutech.careeroracle.domain.port.out;

import com.edutech.careeroracle.domain.model.CollegePrediction;

import java.util.List;
import java.util.UUID;

public interface CollegePredictionRepository {

    CollegePrediction save(CollegePrediction prediction);

    List<CollegePrediction> saveAll(List<CollegePrediction> predictions);

    List<CollegePrediction> findByStudentId(UUID studentId);
}
