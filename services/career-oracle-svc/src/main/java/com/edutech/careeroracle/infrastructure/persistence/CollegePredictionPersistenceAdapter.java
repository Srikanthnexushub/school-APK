package com.edutech.careeroracle.infrastructure.persistence;

import com.edutech.careeroracle.domain.model.CollegePrediction;
import com.edutech.careeroracle.domain.port.out.CollegePredictionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CollegePredictionPersistenceAdapter implements CollegePredictionRepository {

    private final SpringDataCollegePredictionRepository springDataRepository;

    public CollegePredictionPersistenceAdapter(SpringDataCollegePredictionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CollegePrediction save(CollegePrediction prediction) {
        return springDataRepository.save(prediction);
    }

    @Override
    public List<CollegePrediction> saveAll(List<CollegePrediction> predictions) {
        return springDataRepository.saveAll(predictions);
    }

    @Override
    public List<CollegePrediction> findByStudentId(UUID studentId) {
        return springDataRepository.findByStudentId(studentId);
    }
}
