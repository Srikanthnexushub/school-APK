package com.edutech.student.domain.port.out;

import com.edutech.student.domain.model.TargetExam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TargetExamRepository {
    TargetExam save(TargetExam exam);

    Optional<TargetExam> findById(UUID id);

    List<TargetExam> findByStudentId(UUID studentId);

    void deleteById(UUID id);
}
