package com.edutech.curricular.infrastructure.adapter.in.web;

import com.edutech.curricular.application.dto.*;
import com.edutech.curricular.domain.model.CoverageLog;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.port.in.LogCoverageUseCase;
import com.edutech.curricular.domain.port.in.QueryCoverageUseCase;
import com.edutech.curricular.domain.port.out.TopicRepository;
import com.edutech.curricular.infrastructure.config.AuthPrincipal;
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
public class CoverageController {

    private final LogCoverageUseCase logCoverageUseCase;
    private final QueryCoverageUseCase queryCoverageUseCase;
    private final TopicRepository topicRepository;

    @GetMapping("/coverage")
    public ResponseEntity<?> getTeacherCoverage(
            @RequestParam(required = false) UUID subjectId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"TEACHER".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<CoverageLog> logs = queryCoverageUseCase.getTeacherCoverage(principal.userId(), subjectId);
        List<CoverageLogResponse> responses = logs.stream()
            .map(cl -> {
                String topicTitle = topicRepository.findById(cl.getTopicId())
                    .map(Topic::getTopicTitle).orElse("Unknown");
                return new CoverageLogResponse(cl.getId(), cl.getTopicId(), topicTitle, cl.getTaughtOn(), cl.getDeliveryMethod().name(), cl.getDurationMins());
            }).toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/coverage/log")
    public ResponseEntity<?> logCoverage(
            @RequestBody CoverageLogRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"TEACHER".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CoverageLog log = logCoverageUseCase.logCoverage(principal.userId(), principal.centerId(), request);
        String topicTitle = topicRepository.findById(log.getTopicId())
            .map(Topic::getTopicTitle).orElse("Unknown");
        return ResponseEntity.ok(new CoverageLogResponse(log.getId(), log.getTopicId(), topicTitle, log.getTaughtOn(), log.getDeliveryMethod().name(), log.getDurationMins()));
    }

    @GetMapping("/center/{centerId}/report")
    public ResponseEntity<?> getCenterReport(
            @PathVariable UUID centerId,
            @RequestParam UUID subjectId,
            @RequestParam UUID gradeId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"CENTER_ADMIN".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CenterCoverageReport report = queryCoverageUseCase.getCenterReport(centerId, subjectId, gradeId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/institution/summary")
    public ResponseEntity<?> getInstitutionSummary(
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"INSTITUTION_ADMIN".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<CenterCoverageReport> centerReports = queryCoverageUseCase.getInstitutionSummary(principal.userId());
        double avg = centerReports.isEmpty() ? 0.0 :
            centerReports.stream().mapToDouble(CenterCoverageReport::coveragePercent).average().orElse(0.0);
        return ResponseEntity.ok(new InstitutionSummaryResponse(centerReports, avg));
    }
}
