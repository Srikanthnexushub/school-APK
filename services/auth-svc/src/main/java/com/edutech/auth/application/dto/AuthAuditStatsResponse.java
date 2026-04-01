// src/main/java/com/edutech/auth/application/dto/AuthAuditStatsResponse.java
package com.edutech.auth.application.dto;

import java.util.Map;

public record AuthAuditStatsResponse(
    long totalUsers,
    long activeUsers,
    long deletedUsers,
    Map<String, Long> usersByRole,
    long registeredLast7Days,
    long registeredLast30Days,
    long mfaEnabledUsers,
    long socialAuthUsers,
    long verifiedEmailUsers
) {}
