package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.application.dto.CenterCoverageReport;
import com.edutech.curricular.domain.model.CoverageLog;

import java.util.List;
import java.util.UUID;

public interface QueryCoverageUseCase {
    List<CoverageLog> getTeacherCoverage(UUID teacherId, UUID subjectId);
    CenterCoverageReport getCenterReport(UUID centerId, UUID subjectId, UUID gradeId);
    List<CenterCoverageReport> getInstitutionSummary(UUID institutionAdminId);
}
