package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.ExtracurricularActivity;
import com.edutech.curricular.domain.model.enums.ActivityCategory;
import com.edutech.curricular.domain.port.out.ActivityRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaActivityRepository extends JpaRepository<ExtracurricularActivity, UUID>, ActivityRepository {

    @Override
    List<ExtracurricularActivity> findByCenterIdAndActive(UUID centerId, boolean active);

    @Override
    List<ExtracurricularActivity> findByCenterIdAndCategoryAndActive(UUID centerId, ActivityCategory category, boolean active);
}
