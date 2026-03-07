// src/main/java/com/edutech/assess/application/service/QuestionService.java
package com.edutech.assess.application.service;

import com.edutech.assess.application.dto.AddQuestionRequest;
import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.QuestionResponse;
import com.edutech.assess.application.exception.AssessAccessDeniedException;
import com.edutech.assess.application.exception.ExamNotFoundException;
import com.edutech.assess.domain.model.Exam;
import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.port.in.AddQuestionUseCase;
import com.edutech.assess.domain.port.out.ExamRepository;
import com.edutech.assess.domain.port.out.QuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class QuestionService implements AddQuestionUseCase {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository questionRepository,
                           ExamRepository examRepository,
                           ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public QuestionResponse addQuestion(UUID examId, AddQuestionRequest request, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!principal.belongsToCenter(exam.getCenterId())) {
            throw new AssessAccessDeniedException();
        }
        String optionsJson = serializeOptions(request.options());
        Question q = Question.create(
                examId,
                request.questionText(),
                optionsJson,
                request.correctAnswer(),
                request.explanation(),
                request.marks(),
                request.difficulty(),
                request.discrimination(),
                request.guessingParam()
        );
        Question saved = questionRepository.save(q);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(UUID examId, AuthPrincipal principal) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
        if (!principal.belongsToCenter(exam.getCenterId())) {
            throw new AssessAccessDeniedException();
        }
        return questionRepository.findByExamId(examId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String serializeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize options", e);
        }
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private QuestionResponse toResponse(Question q) {
        return new QuestionResponse(
                q.getId(), q.getExamId(), q.getQuestionText(),
                parseOptions(q.getOptionsJson()), q.getCorrectAnswer(), q.getExplanation(),
                q.getMarks(), q.getDifficulty(), q.getDiscrimination(), q.getGuessingParam(),
                q.getCreatedAt()
        );
    }
}
