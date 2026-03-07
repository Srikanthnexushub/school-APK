// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataScheduleRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findByBatchId(UUID batchId);
    List<Schedule> findByCenterId(UUID centerId);
}
