package com.edutech.performance.domain.port.out;

import com.edutech.performance.domain.model.SubjectMastery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectMasteryRepository {

    SubjectMastery save(SubjectMastery mastery);

    List<SubjectMastery> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    Optional<SubjectMastery> findByStudentIdAndEnrollmentIdAndSubject(UUID studentId, UUID enrollmentId, String subject);
}
