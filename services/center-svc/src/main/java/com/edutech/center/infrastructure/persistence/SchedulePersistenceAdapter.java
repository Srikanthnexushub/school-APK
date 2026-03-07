// src/main/java/com/edutech/center/infrastructure/persistence/SchedulePersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Schedule;
import com.edutech.center.domain.port.out.ScheduleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SchedulePersistenceAdapter implements ScheduleRepository {

    private final SpringDataScheduleRepository jpa;

    public SchedulePersistenceAdapter(SpringDataScheduleRepository jpa) { this.jpa = jpa; }

    @Override public Schedule save(Schedule schedule) { return jpa.save(schedule); }
    @Override public Optional<Schedule> findById(UUID id) { return jpa.findById(id); }
    @Override public List<Schedule> findByBatchId(UUID batchId) { return jpa.findByBatchId(batchId); }
    @Override public List<Schedule> findByCenterId(UUID centerId) { return jpa.findByCenterId(centerId); }
}
