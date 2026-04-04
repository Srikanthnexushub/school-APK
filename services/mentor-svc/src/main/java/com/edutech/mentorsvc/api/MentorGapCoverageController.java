package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.domain.model.MentorSession;
import com.edutech.mentorsvc.domain.model.SessionStatus;
import com.edutech.mentorsvc.domain.port.out.MentorSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/mentor-sessions")
public class MentorGapCoverageController {

    private static final Logger log = LoggerFactory.getLogger(MentorGapCoverageController.class);

    private final MentorSessionRepository mentorSessionRepository;

    public MentorGapCoverageController(MentorSessionRepository mentorSessionRepository) {
        this.mentorSessionRepository = mentorSessionRepository;
    }

    public record GapCoverageResponse(
            int totalCompletedSessions,
            int totalStudyMinutes,
            OffsetDateTime lastSessionDate,
            long daysSinceLastSession,
            List<String> coveredTopics
    ) {}

    @GetMapping("/gap-coverage")
    public ResponseEntity<GapCoverageResponse> getGapCoverage(@RequestParam UUID studentId) {
        log.debug("Gap coverage requested for studentId={}", studentId);

        List<MentorSession> completed = mentorSessionRepository.findByStudentId(studentId)
                .stream()
                .filter(s -> s.getDeletedAt() == null && s.getStatus() == SessionStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            log.debug("No completed sessions found for studentId={}", studentId);
            return ResponseEntity.ok(new GapCoverageResponse(0, 0, null, 0L, List.of()));
        }

        int totalCompletedSessions = completed.size();

        int totalStudyMinutes = completed.stream()
                .mapToInt(MentorSession::getDurationMinutes)
                .sum();

        OffsetDateTime lastSessionDate = completed.stream()
                .map(MentorSession::getCompletedAt)
                .filter(dt -> dt != null)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        long daysSinceLastSession = lastSessionDate == null
                ? 0L
                : ChronoUnit.DAYS.between(lastSessionDate.toLocalDate(), OffsetDateTime.now().toLocalDate());

        List<String> coveredTopics = completed.stream()
                .map(MentorSession::getNotes)
                .filter(n -> n != null && !n.isBlank())
                .map(n -> n.trim().length() > 100 ? n.trim().substring(0, 100) : n.trim())
                .distinct()
                .limit(10)
                .collect(Collectors.toList());

        GapCoverageResponse response = new GapCoverageResponse(
                totalCompletedSessions,
                totalStudyMinutes,
                lastSessionDate,
                daysSinceLastSession,
                coveredTopics
        );

        log.debug("Gap coverage for studentId={}: completedSessions={}, studyMinutes={}",
                studentId, totalCompletedSessions, totalStudyMinutes);

        return ResponseEntity.ok(response);
    }
}
