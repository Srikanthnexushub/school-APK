package com.edutech.student.infrastructure.persistence;

import com.edutech.student.domain.model.AcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataAcademicRecordRepository extends JpaRepository<AcademicRecord, UUID> {

    List<AcademicRecord> findByStudentIdAndDeletedAtIsNull(UUID studentId);
}
