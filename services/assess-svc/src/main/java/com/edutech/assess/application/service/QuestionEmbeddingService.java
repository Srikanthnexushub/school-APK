// src/main/java/com/edutech/assess/application/service/QuestionEmbeddingService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.SimilarQuestionRequest;
import com.edutech.assess.application.dto.SimilarQuestionResponse;
import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.port.in.FindSimilarQuestionsUseCase;
import com.edutech.assess.domain.port.out.QuestionSimilaritySearchPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuestionEmbeddingService implements FindSimilarQuestionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(QuestionEmbeddingService.class);

    private final QuestionSimilaritySearchPort similaritySearchPort;
    private final ObjectMapper objectMapper;

    public QuestionEmbeddingService(QuestionSimilaritySearchPort similaritySearchPort,
                                    ObjectMapper objectMapper) {
        this.similaritySearchPort = similaritySearchPort;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimilarQuestionResponse> findSimilarQuestions(SimilarQuestionRequest request) {
        List<Question> results = similaritySearchPort.findSimilar(
                request.queryEmbedding(),
                request.topK(),
                request.excludeIds()
        );
        return results.stream()
                .map(this::toResponse)
                .toList();
    }

    public void updateEmbedding(UUID questionId, float[] embedding) {
        similaritySearchPort.saveEmbedding(questionId, embedding);
        log.info("Saved embedding for question {}", questionId);
    }

    private SimilarQuestionResponse toResponse(Question q) {
        List<String> options = parseOptions(q.getOptionsJson());
        return new SimilarQuestionResponse(
                q.getId(),
                q.getExamId(),
                q.getQuestionText(),
                options,
                q.getMarks(),
                q.getDifficulty(),
                q.getDiscrimination(),
                q.getGuessingParam()
        );
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
