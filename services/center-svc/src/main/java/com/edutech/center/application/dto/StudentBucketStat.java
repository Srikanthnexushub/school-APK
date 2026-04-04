package com.edutech.center.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-student attendance statistics within a single time bucket.
 * {@code atRisk} is true when attendance % < 75.
 * {@code streak} counts consecutive present/late days from the most recent session backwards.
 */
public record StudentBucketStat(
        UUID studentId,
        String studentName,
        long totalSessions,
        long present,
        long absent,
        long late,
        long excused,
        BigDecimal attendancePercent,
        long streak,
        boolean atRisk
) {}
