// src/main/java/com/edutech/center/application/service/BatchService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.UpdateBatchRequest;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.domain.event.BatchCreatedEvent;
import com.edutech.center.domain.event.BatchStatusChangedEvent;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchStatus;
import com.edutech.center.domain.port.in.CreateBatchUseCase;
import com.edutech.center.domain.port.in.UpdateBatchUseCase;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BatchService implements CreateBatchUseCase, UpdateBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private final BatchRepository batchRepository;
    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;

    public BatchService(BatchRepository batchRepository,
                        CenterRepository centerRepository,
                        CenterEventPublisher eventPublisher) {
        this.batchRepository = batchRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public BatchResponse createBatch(UUID centerId, CreateBatchRequest request, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));

        Batch batch = Batch.create(centerId, request.name(), request.code(),
                request.subject(), request.teacherId(), request.maxStudents(),
                request.startDate(), request.endDate());

        Batch saved = batchRepository.save(batch);

        eventPublisher.publish(new BatchCreatedEvent(
                saved.getId(), centerId, saved.getName(),
                saved.getSubject(), saved.getTeacherId()));

        log.info("Batch created: id={} centerId={}", saved.getId(), centerId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(UUID batchId, UpdateBatchRequest request, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        if (!principal.belongsToCenter(batch.getCenterId())) {
            throw new CenterAccessDeniedException();
        }

        BatchStatus previousStatus = batch.getStatus();

        if (request.teacherId() != null) {
            batch.assignTeacher(request.teacherId());
        }

        switch (request.status()) {
            case ACTIVE -> batch.activate();
            case COMPLETED -> batch.complete();
            case CANCELLED -> batch.cancel();
            default -> { /* UPCOMING — no transition needed */ }
        }

        Batch saved = batchRepository.save(batch);

        if (previousStatus != saved.getStatus()) {
            eventPublisher.publish(new BatchStatusChangedEvent(
                    batchId, batch.getCenterId(), previousStatus, saved.getStatus()));
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> listBatches(UUID centerId, BatchStatus status, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        List<Batch> batches = status != null
            ? batchRepository.findByCenterIdAndStatus(centerId, status)
            : batchRepository.findByCenterId(centerId);
        return batches.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<BatchResponse> listBatches(UUID centerId, BatchStatus status, AuthPrincipal principal, Pageable pageable) {
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        List<BatchResponse> all = (status != null
            ? batchRepository.findByCenterIdAndStatus(centerId, status)
            : batchRepository.findByCenterId(centerId))
            .stream().map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatch(UUID centerId, UUID batchId, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        Batch batch = batchRepository.findByIdAndCenterId(batchId, centerId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));
        return toResponse(batch);
    }

    private BatchResponse toResponse(Batch b) {
        return new BatchResponse(b.getId(), b.getCenterId(), b.getName(), b.getCode(),
                b.getSubject(), b.getTeacherId(), b.getMaxStudents(), b.getEnrolledCount(),
                b.getStartDate(), b.getEndDate(), b.getStatus(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
