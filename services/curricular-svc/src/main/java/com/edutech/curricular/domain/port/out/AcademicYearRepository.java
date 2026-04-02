package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.AcademicYear;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository {
    AcademicYear save(AcademicYear academicYear);
    Optional<AcademicYear> findById(UUID id);
    List<AcademicYear> findByCenterIdAndActive(UUID centerId, boolean active);
}
