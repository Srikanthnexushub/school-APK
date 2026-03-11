// src/main/java/com/edutech/parent/infrastructure/persistence/SpringDataStudentLinkRepository.java
package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.StudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataStudentLinkRepository extends JpaRepository<StudentLink, UUID> {

    @Query("SELECT l FROM StudentLink l WHERE l.parentId = :parentId")
    List<StudentLink> findAllByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT l FROM StudentLink l WHERE l.parentId = :parentId AND l.status = 'ACTIVE'")
    List<StudentLink> findActiveByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT l FROM StudentLink l WHERE l.parentId = :parentId AND l.studentId = :studentId AND l.status = 'ACTIVE'")
    Optional<StudentLink> findByParentIdAndStudentId(@Param("parentId") UUID parentId,
                                                     @Param("studentId") UUID studentId);

    @Query("SELECT l FROM StudentLink l WHERE l.studentId = :studentId AND l.status = 'ACTIVE'")
    List<StudentLink> findActiveByStudentId(@Param("studentId") UUID studentId);
}
