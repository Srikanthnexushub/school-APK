// src/main/java/com/edutech/assess/api/QuestionEmbeddingController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.EmbeddingUpdateRequest;
import com.edutech.assess.application.dto.SimilarQuestionRequest;
import com.edutech.assess.application.dto.SimilarQuestionResponse;
import com.edutech.assess.application.service.QuestionEmbeddingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Question Embeddings")
public class QuestionEmbeddingController {

    private final QuestionEmbeddingService questionEmbeddingService;

    public QuestionEmbeddingController(QuestionEmbeddingService questionEmbeddingService) {
        this.questionEmbeddingService = questionEmbeddingService;
    }

    /**
     * Find semantically similar questions using pgvector cosine similarity.
     * Accessible by STUDENT (for CAT recommendations) and ADMIN (for authoring tools).
     */
    @PostMapping("/similar")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public List<SimilarQuestionResponse> findSimilarQuestions(
            @Valid @RequestBody SimilarQuestionRequest request) {
        return questionEmbeddingService.findSimilarQuestions(request);
    }

    /**
     * Store an embedding vector for a question.
     * Called exclusively by the ai-gateway-svc batch embedding job.
     * Auth: ADMIN only.
     */
    @PutMapping("/{id}/embedding")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateEmbedding(
            @PathVariable UUID id,
            @RequestBody EmbeddingUpdateRequest request) {
        questionEmbeddingService.updateEmbedding(id, request.embedding());
        return ResponseEntity.noContent().build();
    }
}
