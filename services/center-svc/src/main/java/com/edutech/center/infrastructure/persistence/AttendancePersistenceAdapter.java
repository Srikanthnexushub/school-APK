// src/main/java/com/edutech/center/infrastructure/persistence/AttendancePersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Attendance;
import com.edutech.center.domain.port.out.AttendanceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class AttendancePersistenceAdapter implements AttendanceRepository {

    private final SpringDataAttendanceRepository jpa;

    public AttendancePersistenceAdapter(SpringDataAttendanceRepository jpa) { this.jpa = jpa; }

    @Override public Attendance save(Attendance a) { return jpa.save(a); }
    @Override public List<Attendance> saveAll(List<Attendance> records) { return jpa.saveAll(records); }
    @Override public List<Attendance> findByBatchIdAndDate(UUID batchId, LocalDate date) { return jpa.findByBatchIdAndDate(batchId, date); }
    @Override public List<Attendance> findByStudentIdAndBatchId(UUID studentId, UUID batchId) { return jpa.findByStudentIdAndBatchId(studentId, batchId); }
    @Override public void deleteByBatchIdAndDate(UUID batchId, LocalDate date) { jpa.deleteByBatchIdAndDate(batchId, date); }
}
