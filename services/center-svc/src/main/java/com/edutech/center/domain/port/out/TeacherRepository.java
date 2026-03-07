// src/main/java/com/edutech/center/domain/port/out/TeacherRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.Teacher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository {
    Teacher save(Teacher teacher);
    Optional<Teacher> findById(UUID id);
    Optional<Teacher> findByIdAndCenterId(UUID id, UUID centerId);
    List<Teacher> findByCenterId(UUID centerId);
    boolean existsByUserIdAndCenterId(UUID userId, UUID centerId);
}
