// src/main/java/com/edutech/center/domain/port/in/UpdateBatchUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.UpdateBatchRequest;

import java.util.UUID;

public interface UpdateBatchUseCase {
    BatchResponse updateBatch(UUID batchId, UpdateBatchRequest request, AuthPrincipal principal);
}
