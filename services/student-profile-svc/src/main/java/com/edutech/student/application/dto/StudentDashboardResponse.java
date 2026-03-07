package com.edutech.student.application.dto;

import java.util.List;

public record StudentDashboardResponse(
        StudentProfileResponse profile,
        List<AcademicRecordResponse> academicHistory,
        List<TargetExamResponse> targetExams,
        Integer totalExamsTargeted
) {}
