// src/main/java/com/edutech/center/infrastructure/persistence/TeacherPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TeacherPersistenceAdapter implements TeacherRepository {

    private final SpringDataTeacherRepository jpa;

    public TeacherPersistenceAdapter(SpringDataTeacherRepository jpa) { this.jpa = jpa; }

    @Override public Teacher save(Teacher teacher) { return jpa.save(teacher); }
    @Override public Optional<Teacher> findById(UUID id) { return jpa.findById(id); }
    @Override public Optional<Teacher> findByIdAndCenterId(UUID id, UUID centerId) { return jpa.findByIdAndCenterIdActive(id, centerId); }
    @Override public List<Teacher> findByCenterId(UUID centerId) { return jpa.findByCenterIdActive(centerId); }
    @Override public boolean existsByUserIdAndCenterId(UUID userId, UUID centerId) { return jpa.existsByUserIdAndCenterId(userId, centerId); }
}
