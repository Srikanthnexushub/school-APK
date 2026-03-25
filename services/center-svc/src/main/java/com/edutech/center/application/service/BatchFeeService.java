// src/main/java/com/edutech/center/application/service/BatchFeeService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AssignFeeRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchFeeAssignmentResponse;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchFeeAssignment;
import com.edutech.center.domain.port.out.BatchFeeAssignmentRepository;
import com.edutech.center.domain.port.out.BatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BatchFeeService {

    private static final Logger log = LoggerFactory.getLogger(BatchFeeService.class);

    private final BatchFeeAssignmentRepository assignmentRepository;
    private final BatchRepository batchRepository;

    public BatchFeeService(BatchFeeAssignmentRepository assignmentRepository,
                           BatchRepository batchRepository) {
        this.assignmentRepository = assignmentRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public BatchFeeAssignmentResponse assign(UUID batchId, AssignFeeRequest request, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        BatchFeeAssignment assignment = BatchFeeAssignment.assign(
                batchId, batch.getCenterId(), request.feeStructureId(),
                request.effectiveFrom(), request.effectiveTo());
        BatchFeeAssignment saved = assignmentRepository.save(assignment);
        log.info("Fee assigned: batchId={} feeStructureId={}", batchId, request.feeStructureId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BatchFeeAssignmentResponse> listByBatch(UUID batchId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        return assignmentRepository.findByBatchId(batchId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void remove(UUID batchId, UUID assignmentId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        assignmentRepository.deleteById(assignmentId);
        log.info("Fee assignment removed: assignmentId={} batchId={}", assignmentId, batchId);
    }

    private BatchFeeAssignmentResponse toResponse(BatchFeeAssignment a) {
        return new BatchFeeAssignmentResponse(a.getId(), a.getBatchId(), a.getCenterId(),
                a.getFeeStructureId(), a.getEffectiveFrom(), a.getEffectiveTo(), a.getCreatedAt());
    }
}
