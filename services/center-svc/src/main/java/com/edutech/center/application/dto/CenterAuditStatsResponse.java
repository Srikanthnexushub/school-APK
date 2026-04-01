// src/main/java/com/edutech/center/application/dto/CenterAuditStatsResponse.java
package com.edutech.center.application.dto;

public record CenterAuditStatsResponse(
    long totalCenters,
    long activeCenters,
    long totalBatches,
    long activeBatches,
    long upcomingBatches,
    long completedBatches,
    long totalEnrolled,
    long totalTeachers,
    long activeTeachers,
    double avgBatchFillRate
) {}
