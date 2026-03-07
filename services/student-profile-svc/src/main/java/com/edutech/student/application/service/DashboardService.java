package com.edutech.student.application.service;

import com.edutech.student.application.dto.AcademicRecordResponse;
import com.edutech.student.application.dto.StudentDashboardResponse;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.application.dto.TargetExamResponse;
import com.edutech.student.domain.port.in.GetStudentDashboardUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService implements GetStudentDashboardUseCase {

    private final StudentProfileService profileService;
    private final AcademicRecordService academicRecordService;
    private final TargetExamService targetExamService;

    public DashboardService(StudentProfileService profileService,
                             AcademicRecordService academicRecordService,
                             TargetExamService targetExamService) {
        this.profileService = profileService;
        this.academicRecordService = academicRecordService;
        this.targetExamService = targetExamService;
    }

    @Override
    public StudentDashboardResponse getDashboard(UUID studentId) {
        StudentProfileResponse profile = profileService.getProfile(studentId);
        List<AcademicRecordResponse> academicHistory = academicRecordService.getRecordsByStudentId(studentId);
        List<TargetExamResponse> targetExams = targetExamService.getExamsByStudentId(studentId);

        return new StudentDashboardResponse(
                profile,
                academicHistory,
                targetExams,
                targetExams.size()
        );
    }
}
