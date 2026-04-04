package com.edutech.center.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One time slice in an attendance period report (e.g. "March 2026"). */
public record AttendanceBucket(
        String label,
        LocalDate from,
        LocalDate to,
        List<StudentBucketStat> students,
        BigDecimal batchAveragePercent
) {}
