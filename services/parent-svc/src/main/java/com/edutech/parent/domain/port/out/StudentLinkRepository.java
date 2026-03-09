// src/main/java/com/edutech/parent/domain/port/out/StudentLinkRepository.java
package com.edutech.parent.domain.port.out;

import com.edutech.parent.domain.model.StudentLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentLinkRepository {
    StudentLink save(StudentLink link);
    Optional<StudentLink> findById(UUID id);
    List<StudentLink> findByParentId(UUID parentId);
    Optional<StudentLink> findByParentIdAndStudentId(UUID parentId, UUID studentId);
    List<StudentLink> findActiveByStudentId(UUID studentId);
    List<StudentLink> findActiveByParentId(UUID parentId);
}
