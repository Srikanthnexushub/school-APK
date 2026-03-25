// src/main/java/com/edutech/center/domain/port/in/AssignFeeToBatchUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AssignFeeRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchFeeAssignmentResponse;

import java.util.List;
import java.util.UUID;

public interface AssignFeeToBatchUseCase {
    BatchFeeAssignmentResponse assign(UUID centerId, UUID batchId, AssignFeeRequest request, AuthPrincipal principal);
    List<BatchFeeAssignmentResponse> listByBatch(UUID centerId, UUID batchId, AuthPrincipal principal);
    void remove(UUID centerId, UUID batchId, UUID assignmentId, AuthPrincipal principal);
}
