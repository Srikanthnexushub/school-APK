package com.edutech.careeroracle.domain.port.in;

import com.edutech.careeroracle.application.dto.CollegePredictionResponse;

import java.util.List;
import java.util.UUID;

public interface PredictCollegesUseCase {

    List<CollegePredictionResponse> predictColleges(UUID studentId);

    List<CollegePredictionResponse> getPredictions(UUID studentId);
}
