// src/main/java/com/edutech/center/application/service/AttendanceSummaryService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AttendanceSummaryResponse;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.domain.model.Attendance;
import com.edutech.center.domain.model.AttendanceStatus;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchMember;
import com.edutech.center.domain.port.out.AttendanceRepository;
import com.edutech.center.domain.port.out.BatchMemberRepository;
import com.edutech.center.domain.port.out.BatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Computes attendance summary per student in a batch.
 * Uses in-memory aggregation over the attendance repository
 * (the DB view is for reporting queries; this service serves API calls).
 */
@Service
public class AttendanceSummaryService {

    private final AttendanceRepository attendanceRepository;
    private final BatchMemberRepository batchMemberRepository;
    private final BatchRepository batchRepository;

    public AttendanceSummaryService(AttendanceRepository attendanceRepository,
                                    BatchMemberRepository batchMemberRepository,
                                    BatchRepository batchRepository) {
        this.attendanceRepository = attendanceRepository;
        this.batchMemberRepository = batchMemberRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getSummary(UUID batchId, AuthPrincipal principal) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));
        if (!principal.belongsToCenter(batch.getCenterId()) && !principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        List<BatchMember> members = batchMemberRepository.findActiveByBatchId(batchId);
        return members.stream().map(m -> computeSummary(batchId, m)).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getMyAttendance(UUID batchId, AuthPrincipal principal) {
        BatchMember me = batchMemberRepository.findByBatchIdAndStudentId(batchId, principal.userId())
                .orElseGet(() -> {
                    BatchMember stub = BatchMember.enroll(batchId, null, principal.userId(), "");
                    return stub;
                });
        return computeSummary(batchId, me);
    }

    private AttendanceSummaryResponse computeSummary(UUID batchId, BatchMember m) {
        List<Attendance> records = attendanceRepository.findByStudentIdAndBatchId(m.getStudentId(), batchId);
        Map<AttendanceStatus, Long> counts = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting()));

        long present  = counts.getOrDefault(AttendanceStatus.PRESENT,  0L);
        long absent   = counts.getOrDefault(AttendanceStatus.ABSENT,   0L);
        long late     = counts.getOrDefault(AttendanceStatus.LATE,     0L);
        long excused  = counts.getOrDefault(AttendanceStatus.EXCUSED,  0L);
        long total    = records.size();

        BigDecimal pct = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf((present + late) * 100.0 / total)
                        .setScale(2, RoundingMode.HALF_UP);

        return new AttendanceSummaryResponse(batchId, m.getStudentId(), m.getStudentName(),
                total, present, absent, late, excused, pct);
    }
}
