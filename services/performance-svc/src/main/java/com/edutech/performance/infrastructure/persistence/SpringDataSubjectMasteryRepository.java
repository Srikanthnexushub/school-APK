package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.model.SubjectMastery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataSubjectMasteryRepository extends JpaRepository<SubjectMastery, UUID> {

    List<SubjectMastery> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    Optional<SubjectMastery> findByStudentIdAndEnrollmentIdAndSubject(UUID studentId, UUID enrollmentId, String subject);

    List<SubjectMastery> findByStudentId(UUID studentId);
}
