// src/main/java/com/edutech/center/application/dto/AttendanceSummaryResponse.java
package com.edutech.center.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AttendanceSummaryResponse(
    UUID batchId,
    UUID studentId,
    String studentName,
    long totalDays,
    long presentDays,
    long absentDays,
    long lateDays,
    long excusedDays,
    BigDecimal attendancePct
) {}
