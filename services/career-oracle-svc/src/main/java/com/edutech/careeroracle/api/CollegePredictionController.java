package com.edutech.careeroracle.api;

import com.edutech.careeroracle.application.dto.CollegePredictionResponse;
import com.edutech.careeroracle.domain.port.in.PredictCollegesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/college-predictions")
@Tag(name = "College Predictions", description = "College admission prediction endpoints")
public class CollegePredictionController {

    private final PredictCollegesUseCase predictCollegesUseCase;

    public CollegePredictionController(PredictCollegesUseCase predictCollegesUseCase) {
        this.predictCollegesUseCase = predictCollegesUseCase;
    }

    @PostMapping("/students/{studentId}/predict")
    @Operation(summary = "Generate college predictions for a student")
    public ResponseEntity<List<CollegePredictionResponse>> predictColleges(@PathVariable UUID studentId) {
        List<CollegePredictionResponse> predictions = predictCollegesUseCase.predictColleges(studentId);
        return ResponseEntity.ok(predictions);
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get college predictions for a student")
    public ResponseEntity<List<CollegePredictionResponse>> getPredictions(@PathVariable UUID studentId) {
        List<CollegePredictionResponse> predictions = predictCollegesUseCase.getPredictions(studentId);
        return ResponseEntity.ok(predictions);
    }
}
