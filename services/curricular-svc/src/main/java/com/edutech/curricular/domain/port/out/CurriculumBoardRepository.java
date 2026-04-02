package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.CurriculumBoard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurriculumBoardRepository {
    CurriculumBoard save(CurriculumBoard board);
    Optional<CurriculumBoard> findById(UUID id);
    List<CurriculumBoard> findAll();
    Optional<CurriculumBoard> findByBoardCode(String boardCode);
    List<CurriculumBoard> findByActive(boolean active);
}
