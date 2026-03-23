package com.edutech.center.infrastructure.http;

import com.edutech.center.domain.model.TaggingResult;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls ai-gateway-svc POST /api/v1/ai/completions to auto-tag content items.
 *
 * System prompt instructs the model to return a strict JSON object so the response
 * can be parsed deterministically. On any error (network, parse, timeout) this
 * adapter returns {@link TaggingResult#empty()} — uploads are NEVER blocked by AI.
 */
@Component
public class AiTaggingAdapter implements AiTaggingPort {

    private static final Logger log = LoggerFactory.getLogger(AiTaggingAdapter.class);

    private static final String SYSTEM_PROMPT = """
            You are an intelligent EduTech document classifier.
            Given a document title and optional description, respond ONLY with a valid JSON object
            (no markdown, no explanation) with these exact keys:
            {
              "subject": "<Physics|Chemistry|Mathematics|Biology|History|Geography|Economics|English|other>",
              "board": "<CBSE|ICSE|IB|STATE|ALL>",
              "examType": "<BOARD_EXAM|JEE|NEET|UPSC|GATE|UNIT_TEST|MOCK|PRACTICE>",
              "difficulty": "<EASY|MEDIUM|HARD>",
              "aiSummary": "<2-3 sentence summary of what this document covers>",
              "suggestedTags": ["tag1","tag2","tag3"]
            }
            Use null for fields you cannot determine from the title/description.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiTaggingAdapter(RestClient aiGatewayRestClient, ObjectMapper objectMapper) {
        this.restClient = aiGatewayRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public TaggingResult suggestTags(String title, String description) {
        try {
            String userMessage = "Title: " + title
                    + (description != null && !description.isBlank()
                       ? "\nDescription: " + description : "");

            Map<String, Object> requestBody = Map.of(
                    "requesterId", "center-svc-content-tagger",
                    "systemPrompt", SYSTEM_PROMPT,
                    "userMessage", userMessage,
                    "maxTokens", 512,
                    "temperature", 0.2
            );

            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/ai/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("content")) {
                log.warn("AI gateway returned no content for title='{}'", title);
                return TaggingResult.empty();
            }

            return parseResponse((String) response.get("content"));

        } catch (Exception e) {
            log.warn("AI tagging failed for title='{}' — graceful degradation: {}", title, e.getMessage());
            return TaggingResult.empty();
        }
    }

    public TaggingResult parseResponse(String content) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(content, Map.class);

            String[] tags = null;
            Object tagsObj = parsed.get("suggestedTags");
            if (tagsObj instanceof List<?> tagList) {
                tags = tagList.stream()
                        .filter(t -> t instanceof String)
                        .map(Object::toString)
                        .toArray(String[]::new);
            }

            return new TaggingResult(
                    (String) parsed.get("subject"),
                    (String) parsed.get("board"),
                    (String) parsed.get("examType"),
                    (String) parsed.get("difficulty"),
                    (String) parsed.get("aiSummary"),
                    tags != null ? tags : new String[0]
            );
        } catch (Exception e) {
            log.warn("Failed to parse AI tagging response — graceful degradation: {}", e.getMessage());
            return TaggingResult.empty();
        }
    }
}
