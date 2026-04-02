package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.CurriculumBoard;
import com.edutech.curricular.domain.port.out.CurriculumBoardRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCurriculumBoardRepository extends JpaRepository<CurriculumBoard, UUID>, CurriculumBoardRepository {

    @Override
    Optional<CurriculumBoard> findByBoardCode(String boardCode);

    @Override
    List<CurriculumBoard> findByActive(boolean active);
}
