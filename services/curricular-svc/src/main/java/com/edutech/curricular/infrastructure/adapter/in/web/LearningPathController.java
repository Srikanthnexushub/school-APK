package com.edutech.curricular.infrastructure.adapter.in.web;

import com.edutech.curricular.application.dto.LearningPathItemResponse;
import com.edutech.curricular.application.dto.LearningPathItemStatusUpdate;
import com.edutech.curricular.application.dto.LearningPathResponse;
import com.edutech.curricular.domain.model.LearningPath;
import com.edutech.curricular.domain.model.LearningPathItem;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.enums.PathItemStatus;
import com.edutech.curricular.domain.port.in.ComputeLearningPathUseCase;
import com.edutech.curricular.domain.port.in.UpdateLearningPathUseCase;
import com.edutech.curricular.domain.port.out.LearningPathItemRepository;
import com.edutech.curricular.domain.port.out.TopicRepository;
import com.edutech.curricular.infrastructure.config.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curricular")
@RequiredArgsConstructor
public class LearningPathController {

    private final ComputeLearningPathUseCase computeUseCase;
    private final UpdateLearningPathUseCase updateUseCase;
    private final LearningPathItemRepository itemRepository;
    private final TopicRepository topicRepository;

    @GetMapping("/my-path")
    public ResponseEntity<?> getMyPath(
            @RequestParam UUID subjectId,
            @RequestParam UUID gradeId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String jwt = extractJwt(request);
        LearningPath path = computeUseCase.computeOrRefresh(principal.userId(), subjectId, gradeId, jwt);
        return ResponseEntity.ok(toResponse(path));
    }

    @PostMapping("/my-path/refresh")
    public ResponseEntity<?> refreshMyPath(
            @RequestParam UUID subjectId,
            @RequestParam UUID gradeId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest request) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String jwt = extractJwt(request);
        LearningPath path = computeUseCase.computeOrRefresh(principal.userId(), subjectId, gradeId, jwt);
        return ResponseEntity.ok(toResponse(path));
    }

    @PatchMapping("/my-path/items/{itemId}")
    public ResponseEntity<?> updateItemStatus(
            @PathVariable UUID itemId,
            @RequestBody LearningPathItemStatusUpdate update,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"STUDENT".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PathItemStatus newStatus = PathItemStatus.valueOf(update.status());
        LearningPathItem item = updateUseCase.updateItemStatus(itemId, principal.userId(), newStatus);
        String topicTitle = topicRepository.findById(item.getTopicId())
            .map(Topic::getTopicTitle).orElse("Unknown");
        String bloomsLevel = topicRepository.findById(item.getTopicId())
            .map(t -> t.getBloomsLevel().name()).orElse("REMEMBER");
        return ResponseEntity.ok(new LearningPathItemResponse(
            item.getId(), item.getTopicId(), topicTitle, bloomsLevel,
            item.getSequenceOrder(), item.getUrgencyScore(), item.getStatus().name()
        ));
    }

    private LearningPathResponse toResponse(LearningPath path) {
        List<LearningPathItem> items = itemRepository.findByPathId(path.getId());
        List<LearningPathItemResponse> itemResponses = items.stream()
            .map(item -> {
                String topicTitle = topicRepository.findById(item.getTopicId())
                    .map(Topic::getTopicTitle).orElse("Unknown");
                String bloomsLevel = topicRepository.findById(item.getTopicId())
                    .map(t -> t.getBloomsLevel().name()).orElse("REMEMBER");
                return new LearningPathItemResponse(
                    item.getId(), item.getTopicId(), topicTitle, bloomsLevel,
                    item.getSequenceOrder(), item.getUrgencyScore(), item.getStatus().name()
                );
            }).toList();
        return new LearningPathResponse(
            path.getId(), path.getStudentId(), path.getSubjectId(), path.getGradeId(),
            path.getComputedAt(), itemResponses
        );
    }

    private String extractJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }
}
