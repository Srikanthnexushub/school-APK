package com.edutech.curricular.application.service;

import com.edutech.curricular.application.dto.CenterCoverageReport;
import com.edutech.curricular.application.dto.CoverageLogRequest;
import com.edutech.curricular.domain.model.Chapter;
import com.edutech.curricular.domain.model.CoverageLog;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.enums.DeliveryMethod;
import com.edutech.curricular.domain.port.in.LogCoverageUseCase;
import com.edutech.curricular.domain.port.in.QueryCoverageUseCase;
import com.edutech.curricular.domain.port.out.ChapterRepository;
import com.edutech.curricular.domain.port.out.CoverageLogRepository;
import com.edutech.curricular.domain.port.out.CurricularEventPublisher;
import com.edutech.curricular.domain.port.out.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoverageService implements LogCoverageUseCase, QueryCoverageUseCase {

    private final CoverageLogRepository coverageLogRepository;
    private final TopicRepository topicRepository;
    private final ChapterRepository chapterRepository;
    private final CurricularEventPublisher eventPublisher;

    @Override
    @Transactional
    public CoverageLog logCoverage(UUID teacherId, UUID centerId, CoverageLogRequest request) {
        topicRepository.findById(request.topicId())
            .orElseThrow(() -> new NoSuchElementException("Topic not found: " + request.topicId()));

        DeliveryMethod deliveryMethod = DeliveryMethod.LECTURE;
        if (request.deliveryMethod() != null) {
            try {
                deliveryMethod = DeliveryMethod.valueOf(request.deliveryMethod());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown delivery method: {}, defaulting to LECTURE", request.deliveryMethod());
            }
        }

        CoverageLog log = CoverageLog.create(
            teacherId, centerId, request.topicId(),
            request.taughtOn(), deliveryMethod,
            request.durationMins(), request.notes()
        );
        CoverageLog saved = coverageLogRepository.save(log);
        eventPublisher.publishCoverageLogged(teacherId, centerId, request.topicId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoverageLog> getTeacherCoverage(UUID teacherId, UUID subjectId) {
        List<CoverageLog> logs = coverageLogRepository.findByTeacherId(teacherId);
        if (subjectId == null) {
            return logs;
        }
        // Filter by subject: get all topic IDs for the subject
        List<Chapter> chapters = chapterRepository.findBySubjectId(subjectId);
        Set<UUID> subjectTopicIds = new HashSet<>();
        for (Chapter chapter : chapters) {
            topicRepository.findByChapterId(chapter.getId()).stream()
                .map(Topic::getId)
                .forEach(subjectTopicIds::add);
        }
        return logs.stream()
            .filter(cl -> subjectTopicIds.contains(cl.getTopicId()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CenterCoverageReport getCenterReport(UUID centerId, UUID subjectId, UUID gradeId) {
        // Get all active topics for subject+grade
        List<Chapter> chapters = chapterRepository.findBySubjectIdAndGradeId(subjectId, gradeId);
        List<Topic> allTopics = new ArrayList<>();
        for (Chapter chapter : chapters) {
            allTopics.addAll(topicRepository.findByChapterId(chapter.getId()).stream()
                .filter(Topic::isActive).toList());
        }

        int totalTopics = allTopics.size();
        if (totalTopics == 0) {
            return new CenterCoverageReport(centerId, subjectId, gradeId, 0, 0, 0.0, List.of());
        }

        List<UUID> topicIds = allTopics.stream().map(Topic::getId).toList();
        List<CoverageLog> centerLogs = coverageLogRepository.findByTopicIdIn(topicIds).stream()
            .filter(cl -> cl.getCenterId().equals(centerId))
            .toList();

        Set<UUID> coveredTopicIds = centerLogs.stream()
            .map(CoverageLog::getTopicId)
            .collect(Collectors.toSet());

        int coveredTopics = coveredTopicIds.size();
        double coveragePercent = totalTopics > 0 ? (100.0 * coveredTopics / totalTopics) : 0.0;

        List<String> uncoveredTopicCodes = allTopics.stream()
            .filter(t -> !coveredTopicIds.contains(t.getId()))
            .map(Topic::getTopicCode)
            .toList();

        return new CenterCoverageReport(centerId, subjectId, gradeId, totalTopics, coveredTopics, coveragePercent, uncoveredTopicCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CenterCoverageReport> getInstitutionSummary(UUID institutionAdminId) {
        // Institution-wide summary requires enumerating all centers.
        // This service does not have a local center registry and does not call center-svc.
        // Returns an empty list; a future implementation can inject a CenterDataPort.
        return List.of();
    }
}
