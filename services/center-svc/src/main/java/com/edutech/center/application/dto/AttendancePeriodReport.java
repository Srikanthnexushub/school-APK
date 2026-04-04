package com.edutech.center.application.dto;

import com.edutech.center.domain.model.AttendancePeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full period-bucketed attendance report for a batch. */
public record AttendancePeriodReport(
        UUID batchId,
        String batchName,
        AttendancePeriod period,
        LocalDate from,
        LocalDate to,
        List<AttendanceBucket> buckets,
        List<UUID> atRiskStudentIds
) {}
