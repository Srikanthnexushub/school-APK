package com.edutech.performance.domain.port.out;

import com.edutech.performance.domain.model.WeakAreaRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeakAreaRepository {

    WeakAreaRecord save(WeakAreaRecord record);

    Optional<WeakAreaRecord> findById(UUID id);

    List<WeakAreaRecord> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    List<WeakAreaRecord> findByStudentIdOrderByMasteryAsc(UUID studentId, int limit);

    List<WeakAreaRecord> findByStudentIdAndSubject(UUID studentId, String subject);
}
