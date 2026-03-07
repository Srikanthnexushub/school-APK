// src/main/java/com/edutech/center/domain/port/out/AttendanceRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository {
    Attendance save(Attendance attendance);
    List<Attendance> saveAll(List<Attendance> records);
    List<Attendance> findByBatchIdAndDate(UUID batchId, LocalDate date);
    List<Attendance> findByStudentIdAndBatchId(UUID studentId, UUID batchId);
    void deleteByBatchIdAndDate(UUID batchId, LocalDate date);
}
