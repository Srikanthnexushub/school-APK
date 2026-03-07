// src/main/java/com/edutech/center/domain/port/in/CreateBatchUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CreateBatchRequest;

import java.util.UUID;

public interface CreateBatchUseCase {
    BatchResponse createBatch(UUID centerId, CreateBatchRequest request, AuthPrincipal principal);
}
