package com.edutech.aigateway.application.service;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.GeneratedQuestion;
import com.edutech.aigateway.domain.model.QuestionGenerationRequest;
import com.edutech.aigateway.domain.port.in.GenerateQuestionsUseCase;
import com.edutech.aigateway.domain.port.in.RouteCompletionUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionGenerationService implements GenerateQuestionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RouteCompletionUseCase routeCompletionUseCase;

    public QuestionGenerationService(RouteCompletionUseCase routeCompletionUseCase) {
        this.routeCompletionUseCase = routeCompletionUseCase;
    }

    @Override
    public Mono<List<GeneratedQuestion>> generateQuestions(QuestionGenerationRequest request, AuthPrincipal principal) {
        String systemPrompt = buildSystemPrompt(request);
        String userMessage = "Generate " + request.count() + " MCQ questions on " + request.topic();

        CompletionRequest completionRequest = new CompletionRequest(
                principal.userId().toString(),
                systemPrompt,
                userMessage,
                2000,
                0.7
        );

        return routeCompletionUseCase.routeCompletion(completionRequest, principal)
                .map(response -> parseQuestions(response.content(), request))
                .onErrorResume(e -> {
                    log.warn("Question generation LLM call failed, using fallback: {}", e.getMessage());
                    return Mono.just(buildFallbackQuestions(request));
                });
    }

    private String buildSystemPrompt(QuestionGenerationRequest request) {
        return ("You are an expert question generator for educational assessments. " +
                "question generator mode: true. " +
                "Generate exactly " + request.count() + " multiple-choice questions about the topic: \"" + request.topic() + "\". " +
                "Difficulty level: " + request.difficulty() + ". " +
                "Return ONLY a valid JSON array with no markdown, no code block, no extra text. " +
                "Use this exact structure: " +
                "[{\"questionText\":\"...\",\"options\":[\"A) ...\",\"B) ...\",\"C) ...\",\"D) ...\"]," +
                "\"correctAnswer\":0,\"explanation\":\"...\",\"difficulty\":\"" + request.difficulty() + "\"}] " +
                "correctAnswer is the 0-based index of the correct option (0=A, 1=B, 2=C, 3=D).");
    }

    private List<GeneratedQuestion> parseQuestions(String content, QuestionGenerationRequest fallbackRequest) {
        if (content == null || content.isBlank()) {
            return buildFallbackQuestions(fallbackRequest);
        }
        try {
            String json = extractJsonArray(content);
            return MAPPER.readValue(json, new TypeReference<List<GeneratedQuestion>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse LLM question JSON, using fallback: {}", e.getMessage());
            return buildFallbackQuestions(fallbackRequest);
        }
    }

    private String extractJsonArray(String content) {
        // Strip markdown code blocks if present
        String stripped = content.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("(?s)```$", "").trim();
        }
        // Find the first [ ... ] span
        int start = stripped.indexOf('[');
        int end = stripped.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1);
        }
        return stripped;
    }

    private List<GeneratedQuestion> buildFallbackQuestions(QuestionGenerationRequest request) {
        String topic = request.topic() != null ? request.topic() : "General Knowledge";
        String difficulty = request.difficulty() != null ? request.difficulty() : "MEDIUM";
        int count = Math.max(1, Math.min(request.count(), 20));

        String[] questionTexts = {
            "What is the fundamental concept underlying " + topic + "?",
            "Which of the following best describes a key property of " + topic + "?",
            "In the context of " + topic + ", which statement is correct?",
            "A student studying " + topic + " needs to solve a problem. What is the best approach?",
            "Which concept is most closely associated with " + topic + "?"
        };
        String[][] options = {
            {"A) Conservation principle", "B) Entropy principle", "C) Uncertainty principle", "D) Relativity theory"},
            {"A) It is always linear", "B) It follows predictable patterns", "C) It is purely theoretical", "D) It has no applications"},
            {"A) All variables are independent", "B) Cause and effect are always direct", "C) Multiple factors interact to produce outcomes", "D) Single-factor analysis is sufficient"},
            {"A) Apply the simplest formula", "B) Identify variables and apply relevant principles", "C) Memorize all outcomes", "D) Avoid complex calculations"},
            {"A) Randomness", "B) Equilibrium and balance", "C) Linear progression", "D) Absolute certainty"}
        };
        int[] correct = {0, 1, 2, 1, 1};
        String[] explanations = {
            "The fundamental principle in " + topic + " relates to conservation laws and systematic analysis.",
            topic + " follows evidence-based patterns that can be studied and reliably applied.",
            "In complex domains like " + topic + ", multiple factors interact — single-variable analysis oversimplifies.",
            "Systematic identification of variables and application of domain principles is the correct scientific approach.",
            topic + " is fundamentally concerned with equilibrium between competing forces or factors."
        };

        java.util.List<GeneratedQuestion> result = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            int idx = i % 5;
            result.add(new GeneratedQuestion(
                    questionTexts[idx],
                    java.util.Arrays.asList(options[idx]),
                    correct[idx],
                    explanations[idx],
                    difficulty
            ));
        }
        return result;
    }
}
