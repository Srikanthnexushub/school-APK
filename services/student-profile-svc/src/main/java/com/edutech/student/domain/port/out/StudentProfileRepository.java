package com.edutech.student.domain.port.out;

import com.edutech.student.domain.model.StudentProfile;

import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository {
    StudentProfile save(StudentProfile profile);

    Optional<StudentProfile> findById(UUID id);

    Optional<StudentProfile> findByUserId(UUID userId);

    Optional<StudentProfile> findByEmail(String email);
}
