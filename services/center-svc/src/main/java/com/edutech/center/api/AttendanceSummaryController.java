// src/main/java/com/edutech/center/api/AttendanceSummaryController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.*;
import com.edutech.center.application.service.AttendancePdfExportService;
import com.edutech.center.application.service.AttendanceReportService;
import com.edutech.center.application.service.AttendanceSummaryService;
import com.edutech.center.domain.model.AttendancePeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/batches/{batchId}/attendance")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Attendance Summary", description = "Attendance percentage, period reports, exports, AI insights, and parent notifications")
public class AttendanceSummaryController {

    private final AttendanceSummaryService   summaryService;
    private final AttendanceReportService    reportService;
    private final AttendancePdfExportService pdfExportService;

    public AttendanceSummaryController(AttendanceSummaryService summaryService,
                                       AttendanceReportService reportService,
                                       AttendancePdfExportService pdfExportService) {
        this.summaryService   = summaryService;
        this.reportService    = reportService;
        this.pdfExportService = pdfExportService;
    }

    // ─── Summary ──────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    @Operation(summary = "All-time attendance summary per student (TEACHER / CENTER_ADMIN)")
    public List<AttendanceSummaryResponse> getSummary(@PathVariable UUID centerId,
                                                      @PathVariable UUID batchId,
                                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return summaryService.getSummary(batchId, principal);
    }

    @GetMapping("/my-summary")
    @Operation(summary = "Requesting student's own attendance summary for this batch")
    public List<AttendanceSummaryResponse> getMyAttendance(@PathVariable UUID centerId,
                                                           @PathVariable UUID batchId,
                                                           @AuthenticationPrincipal AuthPrincipal principal) {
        return List.of(summaryService.getMyAttendance(batchId, principal));
    }

    // ─── Period report ────────────────────────────────────────────────────────

    @GetMapping("/report")
    @Operation(summary = "Period-bucketed attendance report (DAILY/WEEKLY/MONTHLY/QUARTERLY/CUSTOM)")
    public AttendancePeriodReport getReport(
            @PathVariable UUID centerId,
            @PathVariable UUID batchId,
            @RequestParam(defaultValue = "MONTHLY") AttendancePeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return reportService.getReport(centerId, batchId, period, from, to, principal);
    }

    // ─── Export (CSV or PDF) ─────────────────────────────────────────────────

    @GetMapping("/report/export")
    @Operation(summary = "Export attendance report as CSV or PDF (format=CSV|PDF)")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable UUID centerId,
            @PathVariable UUID batchId,
            @RequestParam(defaultValue = "MONTHLY") AttendancePeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "CSV") String format,
            @AuthenticationPrincipal AuthPrincipal principal) {

        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        if ("PDF".equalsIgnoreCase(format)) {
            AttendancePeriodReport report = reportService.getReport(centerId, batchId, period, from, to, principal);
            String narration = reportService.generateReportNarration(report);
            byte[] pdf = pdfExportService.generate(report, narration);
            String filename = "attendance-report-" + batchId + "-" + dateStr + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }

        // Default: CSV
        byte[] csv = reportService.exportCsv(centerId, batchId, period, from, to, principal);
        String filename = "attendance-report-" + batchId + "-" + dateStr + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ─── AI insights ─────────────────────────────────────────────────────────

    @GetMapping("/ai-insights")
    @Operation(summary = "AI-powered attendance risk insights per student")
    public List<AiAttendanceInsight> getAiInsights(
            @PathVariable UUID centerId,
            @PathVariable UUID batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return reportService.getAiInsights(centerId, batchId, from, to, principal);
    }

    // ─── Notify parents ───────────────────────────────────────────────────────

    @PostMapping("/report/notify-parents")
    @Operation(summary = "Send batch attendance report summary notification to all parents in the batch")
    public ResponseEntity<Void> notifyParents(
            @PathVariable UUID centerId,
            @PathVariable UUID batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthPrincipal principal) {
        reportService.notifyParents(centerId, batchId, from, to, principal);
        return ResponseEntity.accepted().build();
    }
}
