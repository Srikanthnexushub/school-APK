package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.Subject;
import com.edutech.curricular.domain.port.out.SubjectRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSubjectRepository extends JpaRepository<Subject, UUID>, SubjectRepository {

    @Override
    List<Subject> findByBoardId(UUID boardId);

    @Override
    Optional<Subject> findByBoardIdAndSubjectCode(UUID boardId, String subjectCode);
}
