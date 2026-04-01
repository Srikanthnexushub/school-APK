// src/main/java/com/edutech/center/api/AdminAuditController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.CenterAuditStatsResponse;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/centers/admin/audit")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin Audit", description = "NFR audit statistics for SUPER_ADMIN")
public class AdminAuditController {

    private final CenterRepository centerRepository;
    private final BatchRepository batchRepository;
    private final TeacherRepository teacherRepository;

    public AdminAuditController(CenterRepository centerRepository,
                                BatchRepository batchRepository,
                                TeacherRepository teacherRepository) {
        this.centerRepository  = centerRepository;
        this.batchRepository   = batchRepository;
        this.teacherRepository = teacherRepository;
    }

    @GetMapping("/stats")
    @Operation(summary = "NFR audit stats for SUPER_ADMIN")
    public CenterAuditStatsResponse getAuditStats(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null || !principal.isSuperAdmin()) {
            throw new AccessDeniedException("SUPER_ADMIN only");
        }
        long totalCenters    = centerRepository.countTotal();
        long activeCenters   = centerRepository.countActiveCenters();
        long totalBatches    = batchRepository.countAllActive();
        long activeBatches   = batchRepository.countByStatusActive();
        long upcomingBatches = batchRepository.countByStatusUpcoming();
        long completedBatches = batchRepository.countByStatusCompleted();
        long totalEnrolled   = Optional.ofNullable(batchRepository.sumEnrolledCount()).orElse(0L);
        long totalTeachers   = teacherRepository.countAllActive();
        long activeTeachers  = teacherRepository.countByStatusActive();
        double avgFillRate   = Optional.ofNullable(batchRepository.avgFillRate()).orElse(0.0);

        return new CenterAuditStatsResponse(
                totalCenters, activeCenters, totalBatches,
                activeBatches, upcomingBatches, completedBatches,
                totalEnrolled, totalTeachers, activeTeachers, avgFillRate);
    }
}
