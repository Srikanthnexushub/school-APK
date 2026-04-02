package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.CoverageLog;

import java.util.List;
import java.util.UUID;

public interface CoverageLogRepository {
    CoverageLog save(CoverageLog coverageLog);
    List<CoverageLog> findByTeacherId(UUID teacherId);
    List<CoverageLog> findByCenterIdAndTopicId(UUID centerId, UUID topicId);
    List<CoverageLog> findByCenterId(UUID centerId);
    List<CoverageLog> findByTopicIdIn(List<UUID> topicIds);
}
