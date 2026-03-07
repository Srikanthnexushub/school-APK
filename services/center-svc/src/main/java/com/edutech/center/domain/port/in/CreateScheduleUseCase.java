// src/main/java/com/edutech/center/domain/port/in/CreateScheduleUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CreateScheduleRequest;
import com.edutech.center.application.dto.ScheduleResponse;

import java.util.UUID;

public interface CreateScheduleUseCase {
    ScheduleResponse createSchedule(UUID batchId, CreateScheduleRequest request, AuthPrincipal principal);
}
