// src/main/java/com/edutech/center/application/service/AttendanceService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AttendanceEntry;
import com.edutech.center.application.dto.AttendanceResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.MarkAttendanceRequest;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Attendance;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.port.in.MarkAttendanceUseCase;
import com.edutech.center.domain.port.out.AttendanceRepository;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceService implements MarkAttendanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final BatchRepository batchRepository;
    private final TeacherRepository teacherRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             BatchRepository batchRepository,
                             TeacherRepository teacherRepository) {
        this.attendanceRepository = attendanceRepository;
        this.batchRepository = batchRepository;
        this.teacherRepository = teacherRepository;
    }

    @Override
    @Transactional
    public List<AttendanceResponse> markAttendance(UUID batchId, MarkAttendanceRequest request,
                                                   AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        if (!principal.belongsToCenter(batch.getCenterId())) {
            // TEACHER: JWT centerId is null post-approval; verify via DB lookup
            if (principal.isTeacher()) {
                boolean belongs = teacherRepository.findByUserId(principal.userId())
                        .stream().anyMatch(t -> t.getCenterId().equals(batch.getCenterId()));
                if (!belongs) throw new CenterAccessDeniedException();
            } else {
                throw new CenterAccessDeniedException();
            }
        }

        // Re-marking replaces existing records for the date
        attendanceRepository.deleteByBatchIdAndDate(batchId, request.date());

        List<Attendance> records = request.entries().stream()
            .map(entry -> Attendance.mark(batchId, batch.getCenterId(),
                    entry.studentId(), request.date(),
                    entry.status(), principal.userId(), entry.notes()))
            .toList();

        List<Attendance> saved = attendanceRepository.saveAll(records);

        log.info("Attendance marked: batchId={} date={} count={}",
                batchId, request.date(), saved.size());
        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendance(UUID batchId, LocalDate date, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId())) {
            // TEACHER: JWT centerId is null post-approval; verify via DB lookup
            if (principal.isTeacher()) {
                boolean belongs = teacherRepository.findByUserId(principal.userId())
                        .stream().anyMatch(t -> t.getCenterId().equals(batch.getCenterId()));
                if (!belongs) throw new CenterAccessDeniedException();
            } else {
                throw new CenterAccessDeniedException();
            }
        }
        return attendanceRepository.findByBatchIdAndDate(batchId, date)
                .stream().map(this::toResponse).toList();
    }

    private AttendanceResponse toResponse(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getBatchId(), a.getStudentId(),
                a.getDate(), a.getStatus(), a.getMarkedByTeacherId(),
                a.getNotes(), a.getCreatedAt());
    }
}
