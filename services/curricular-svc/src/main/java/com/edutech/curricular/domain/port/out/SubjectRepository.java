package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.Subject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    Subject save(Subject subject);
    Optional<Subject> findById(UUID id);
    List<Subject> findByBoardId(UUID boardId);
    Optional<Subject> findByBoardIdAndSubjectCode(UUID boardId, String subjectCode);
}
