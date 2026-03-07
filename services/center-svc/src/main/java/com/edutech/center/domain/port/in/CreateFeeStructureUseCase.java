// src/main/java/com/edutech/center/domain/port/in/CreateFeeStructureUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateFeeStructureRequest;
import com.edutech.center.application.dto.FeeStructureResponse;

import java.util.UUID;

public interface CreateFeeStructureUseCase {
    FeeStructureResponse createFeeStructure(UUID centerId, CreateFeeStructureRequest request, AuthPrincipal principal);
}
