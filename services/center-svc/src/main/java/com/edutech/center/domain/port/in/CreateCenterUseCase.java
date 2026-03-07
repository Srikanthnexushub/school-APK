// src/main/java/com/edutech/center/domain/port/in/CreateCenterUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterResponse;
import com.edutech.center.application.dto.CreateCenterRequest;

public interface CreateCenterUseCase {
    CenterResponse createCenter(CreateCenterRequest request, AuthPrincipal principal);
}
