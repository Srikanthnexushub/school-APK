package com.edutech.examtracker.domain.port.out;

import com.edutech.examtracker.domain.model.SyllabusModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusModuleRepository {

    SyllabusModule save(SyllabusModule module);

    Optional<SyllabusModule> findById(UUID id);

    List<SyllabusModule> findByEnrollmentId(UUID enrollmentId);

    List<SyllabusModule> findByStudentIdAndSubject(UUID studentId, String subject);
}
