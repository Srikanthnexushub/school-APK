package com.edutech.performance.domain.port.in;

import com.edutech.performance.application.dto.PerformanceDashboardResponse;

import java.util.UUID;

public interface GetPerformanceDashboardUseCase {

    PerformanceDashboardResponse getDashboard(UUID studentId, UUID enrollmentId);
}
