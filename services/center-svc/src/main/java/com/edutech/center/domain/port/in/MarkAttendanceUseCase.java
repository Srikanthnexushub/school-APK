// src/main/java/com/edutech/center/domain/port/in/MarkAttendanceUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AttendanceResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.MarkAttendanceRequest;

import java.util.List;
import java.util.UUID;

public interface MarkAttendanceUseCase {
    List<AttendanceResponse> markAttendance(UUID batchId, MarkAttendanceRequest request, AuthPrincipal principal);
}
