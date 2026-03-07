package com.edutech.examtracker.domain.port.out;

import com.edutech.examtracker.domain.model.ExamCode;
import com.edutech.examtracker.domain.model.ExamEnrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamEnrollmentRepository {

    ExamEnrollment save(ExamEnrollment enrollment);

    Optional<ExamEnrollment> findById(UUID id);

    List<ExamEnrollment> findByStudentId(UUID studentId);

    Optional<ExamEnrollment> findByStudentIdAndExamCode(UUID studentId, ExamCode examCode);
}
