// src/main/java/com/edutech/center/domain/port/out/ScheduleRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.Schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository {
    Schedule save(Schedule schedule);
    Optional<Schedule> findById(UUID id);
    List<Schedule> findByBatchId(UUID batchId);
    List<Schedule> findByCenterId(UUID centerId);
}
