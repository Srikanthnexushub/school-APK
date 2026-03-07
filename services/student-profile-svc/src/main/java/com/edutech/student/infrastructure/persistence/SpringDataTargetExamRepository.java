package com.edutech.student.infrastructure.persistence;

import com.edutech.student.domain.model.TargetExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataTargetExamRepository extends JpaRepository<TargetExam, UUID> {

    List<TargetExam> findByStudentIdAndDeletedAtIsNull(UUID studentId);
}
