package com.edutech.student.infrastructure.persistence;

import com.edutech.student.domain.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataStudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Optional<StudentProfile> findByIdAndDeletedAtIsNull(UUID id);

    Optional<StudentProfile> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<StudentProfile> findByEmailAndDeletedAtIsNull(String email);

    Optional<StudentProfile> findByParentLinkCodeAndDeletedAtIsNull(String code);
}
