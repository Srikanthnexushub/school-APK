package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.ExamCode;
import com.edutech.examtracker.domain.model.ExamEnrollment;
import com.edutech.examtracker.domain.port.out.ExamEnrollmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExamEnrollmentPersistenceAdapter implements ExamEnrollmentRepository {

    private final SpringDataExamEnrollmentRepository jpa;

    public ExamEnrollmentPersistenceAdapter(SpringDataExamEnrollmentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ExamEnrollment save(ExamEnrollment enrollment) {
        return jpa.save(enrollment);
    }

    @Override
    public Optional<ExamEnrollment> findById(UUID id) {
        return jpa.findByIdActive(id);
    }

    @Override
    public List<ExamEnrollment> findByStudentId(UUID studentId) {
        return jpa.findByStudentIdActive(studentId);
    }

    @Override
    public Optional<ExamEnrollment> findByStudentIdAndExamCode(UUID studentId, ExamCode examCode) {
        return jpa.findByStudentIdAndExamCodeActive(studentId, examCode);
    }
}
