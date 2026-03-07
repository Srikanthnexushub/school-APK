// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataAttendanceRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface SpringDataAttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByBatchIdAndDate(UUID batchId, LocalDate date);

    List<Attendance> findByStudentIdAndBatchId(UUID studentId, UUID batchId);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.batchId = :batchId AND a.date = :date")
    void deleteByBatchIdAndDate(UUID batchId, LocalDate date);
}
