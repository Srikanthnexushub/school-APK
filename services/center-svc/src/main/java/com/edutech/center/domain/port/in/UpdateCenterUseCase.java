// src/main/java/com/edutech/center/domain/port/in/UpdateCenterUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.UpdateCenterRequest;

import java.util.UUID;

public interface UpdateCenterUseCase {
    CenterResponse updateCenter(UUID centerId, UpdateCenterRequest request, AuthPrincipal principal);
}
