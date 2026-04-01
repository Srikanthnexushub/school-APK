package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProactiveNudgeService {

    private final ProactiveNudgeRepository nudgeRepo;
    private final ChatEventPublisherPort eventPublisher;

    @Transactional
    public void handleExamSubmitted(UUID studentId, String examTitle, double percentage,
                                     int wrongCount, UUID examId) {
        String message = buildExamNudge(examTitle, percentage, wrongCount);
        String actionUrl = "/chat?nudge_exam=" + examId;

        ProactiveNudge nudge = new ProactiveNudge();
        nudge.setUserId(studentId);
        nudge.setTriggerType(NudgeTriggerType.EXAM_SUBMITTED);
        nudge.setTriggerPayload(Map.of(
            "examId", examId.toString(),
            "examTitle", examTitle,
            "percentage", percentage,
            "wrongCount", wrongCount
        ));
        nudge.setMessage(message);
        nudge.setActionUrl(actionUrl);
        ProactiveNudge saved = nudgeRepo.save(nudge);

        eventPublisher.publishNudgeNotification(saved);
        saved.markDelivered();
        nudgeRepo.save(saved);
        log.info("Proactive nudge delivered to student {} for exam {}", studentId, examTitle);
    }

    @Transactional
    public void handleWeakAreaCritical(UUID studentId, String subject, String topic) {
        String message = "\u26a0\ufe0f " + subject + " \u2014 " + topic +
            " has dropped to a critical weak area. " +
            "Let me build you a focused recovery plan. Open NexusChat to get started.";

        ProactiveNudge nudge = new ProactiveNudge();
        nudge.setUserId(studentId);
        nudge.setTriggerType(NudgeTriggerType.WEAK_AREA_CRITICAL);
        nudge.setTriggerPayload(Map.of("subject", subject, "topic", topic));
        nudge.setMessage(message);
        nudge.setActionUrl("/chat?page=performance");
        ProactiveNudge saved = nudgeRepo.save(nudge);

        eventPublisher.publishNudgeNotification(saved);
        saved.markDelivered();
        nudgeRepo.save(saved);
        log.info("Weak-area-critical nudge delivered to student {} for {}/{}", studentId, subject, topic);
    }

    private String buildExamNudge(String examTitle, double percentage, int wrongCount) {
        if (percentage >= 85) {
            return "Great work on " + examTitle + "! You scored " +
                String.format("%.0f", percentage) + "%. " +
                "Want me to reinforce the " + wrongCount + " questions you got wrong to reach 100%?";
        } else if (percentage >= 60) {
            return "You completed " + examTitle + " with " +
                String.format("%.0f", percentage) + "%. There are " + wrongCount +
                " questions worth reviewing. Want a quick debrief?";
        } else {
            return "You completed " + examTitle + ". The score was " +
                String.format("%.0f", percentage) + "% \u2014 let's work through the " +
                wrongCount + " questions you missed together. Open NexusChat for a detailed debrief.";
        }
    }
}
