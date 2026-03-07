// src/main/java/com/edutech/center/application/service/ScheduleService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateScheduleRequest;
import com.edutech.center.application.dto.ScheduleResponse;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.ScheduleConflictException;
import com.edutech.center.domain.event.ScheduleChangedEvent;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.Schedule;
import com.edutech.center.domain.port.in.CreateScheduleUseCase;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService implements CreateScheduleUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepository;
    private final BatchRepository batchRepository;
    private final CenterEventPublisher eventPublisher;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           BatchRepository batchRepository,
                           CenterEventPublisher eventPublisher) {
        this.scheduleRepository = scheduleRepository;
        this.batchRepository = batchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ScheduleResponse createSchedule(UUID batchId, CreateScheduleRequest request, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        if (!principal.belongsToCenter(batch.getCenterId())) {
            throw new CenterAccessDeniedException();
        }

        Schedule candidate = Schedule.create(batchId, batch.getCenterId(),
                request.dayOfWeek(), request.startTime(), request.endTime(),
                request.room(), request.effectiveFrom(), request.effectiveTo());

        List<Schedule> existing = scheduleRepository.findByCenterId(batch.getCenterId());
        boolean conflict = existing.stream()
            .filter(s -> !s.getBatchId().equals(batchId))
            .anyMatch(candidate::overlapsWith);

        if (conflict) {
            throw new ScheduleConflictException(request.room(), request.dayOfWeek().name());
        }

        Schedule saved = scheduleRepository.save(candidate);

        eventPublisher.publish(new ScheduleChangedEvent(
                saved.getId(), batchId, batch.getCenterId(), "CREATED"));

        log.info("Schedule created: id={} batchId={}", saved.getId(), batchId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedules(UUID batchId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId())) {
            throw new CenterAccessDeniedException();
        }
        return scheduleRepository.findByBatchId(batchId).stream().map(this::toResponse).toList();
    }

    private ScheduleResponse toResponse(Schedule s) {
        return new ScheduleResponse(s.getId(), s.getBatchId(), s.getCenterId(),
                s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.getRoom(),
                s.getEffectiveFrom(), s.getEffectiveTo(), s.getCreatedAt());
    }
}
