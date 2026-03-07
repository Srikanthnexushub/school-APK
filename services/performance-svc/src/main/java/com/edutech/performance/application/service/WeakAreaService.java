package com.edutech.performance.application.service;

import com.edutech.performance.application.dto.RecordWeakAreaRequest;
import com.edutech.performance.application.dto.WeakAreaResponse;
import com.edutech.performance.domain.event.WeakAreaDetectedEvent;
import com.edutech.performance.domain.model.WeakAreaRecord;
import com.edutech.performance.domain.port.in.GetWeakAreasUseCase;
import com.edutech.performance.domain.port.in.RecordWeakAreaUseCase;
import com.edutech.performance.domain.port.out.PerformanceEventPublisher;
import com.edutech.performance.domain.port.out.WeakAreaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WeakAreaService implements RecordWeakAreaUseCase, GetWeakAreasUseCase {

    private static final Logger log = LoggerFactory.getLogger(WeakAreaService.class);

    private static final BigDecimal WEAK_AREA_THRESHOLD = new BigDecimal("60");

    private final WeakAreaRepository weakAreaRepository;
    private final PerformanceEventPublisher eventPublisher;

    public WeakAreaService(WeakAreaRepository weakAreaRepository,
                            PerformanceEventPublisher eventPublisher) {
        this.weakAreaRepository = weakAreaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public WeakAreaResponse recordWeakArea(UUID studentId, RecordWeakAreaRequest request) {
        WeakAreaRecord record = WeakAreaRecord.detect(
                studentId,
                request.enrollmentId(),
                request.subject(),
                request.topicName(),
                request.masteryPercent(),
                request.primaryErrorType()
        );

        if (request.chapterName() != null) {
            record.setChapterName(request.chapterName());
        }
        record.updateAttempts(request.incorrectAttempts(), request.totalAttempts());
        if (request.prerequisitesWeak() != null) {
            record.markPrerequisitesWeak(request.prerequisitesWeak());
        }

        WeakAreaRecord saved = weakAreaRepository.save(record);
        log.info("Recorded weak area for studentId={} subject={} mastery={}",
                studentId, request.subject(), request.masteryPercent());

        if (request.masteryPercent().compareTo(WEAK_AREA_THRESHOLD) < 0) {
            eventPublisher.publish(new WeakAreaDetectedEvent(
                    UUID.randomUUID().toString(),
                    studentId,
                    request.enrollmentId(),
                    request.subject(),
                    request.topicName(),
                    request.masteryPercent(),
                    request.primaryErrorType(),
                    Instant.now()
            ));
            log.info("Published WeakAreaDetectedEvent for studentId={} subject={}", studentId, request.subject());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeakAreaResponse> getWeakAreas(UUID studentId, UUID enrollmentId) {
        return weakAreaRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeakAreaResponse> getTopWeakAreas(UUID studentId, UUID enrollmentId, int limit) {
        return weakAreaRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .stream()
                .filter(r -> r.getDeletedAt() == null)
                .sorted(Comparator.comparing(WeakAreaRecord::getMasteryPercent))
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private WeakAreaResponse toResponse(WeakAreaRecord record) {
        return new WeakAreaResponse(
                record.getId(),
                record.getStudentId(),
                record.getSubject(),
                record.getTopicName(),
                record.getMasteryPercent(),
                record.getPrimaryErrorType(),
                record.getIncorrectAttempts(),
                record.getTotalAttempts(),
                record.getPrerequisitesWeak(),
                record.getDetectedAt()
        );
    }
}
