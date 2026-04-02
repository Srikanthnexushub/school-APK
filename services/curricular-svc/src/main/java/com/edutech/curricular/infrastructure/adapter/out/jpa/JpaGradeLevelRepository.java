package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.GradeLevel;
import com.edutech.curricular.domain.port.out.GradeLevelRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaGradeLevelRepository extends JpaRepository<GradeLevel, UUID>, GradeLevelRepository {

    @Override
    List<GradeLevel> findByBoardId(UUID boardId);
}
