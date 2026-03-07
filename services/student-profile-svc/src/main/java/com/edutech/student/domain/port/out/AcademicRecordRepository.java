package com.edutech.student.domain.port.out;

import com.edutech.student.domain.model.AcademicRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicRecordRepository {
    AcademicRecord save(AcademicRecord record);

    Optional<AcademicRecord> findById(UUID id);

    List<AcademicRecord> findByStudentId(UUID studentId);
}
