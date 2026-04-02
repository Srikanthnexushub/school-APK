package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.ExtracurricularActivity;
import com.edutech.curricular.domain.model.enums.ActivityCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository {
    ExtracurricularActivity save(ExtracurricularActivity activity);
    Optional<ExtracurricularActivity> findById(UUID id);
    List<ExtracurricularActivity> findByCenterIdAndActive(UUID centerId, boolean active);
    List<ExtracurricularActivity> findByCenterIdAndCategoryAndActive(UUID centerId, ActivityCategory category, boolean active);
}
