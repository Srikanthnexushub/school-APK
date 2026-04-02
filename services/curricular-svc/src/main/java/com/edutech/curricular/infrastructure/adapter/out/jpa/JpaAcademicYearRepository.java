package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.AcademicYear;
import com.edutech.curricular.domain.port.out.AcademicYearRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAcademicYearRepository extends JpaRepository<AcademicYear, UUID>, AcademicYearRepository {

    @Override
    List<AcademicYear> findByCenterIdAndActive(UUID centerId, boolean active);
}
