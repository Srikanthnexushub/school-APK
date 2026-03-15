// src/main/java/com/edutech/center/domain/event/TeacherPendingApprovalEvent.java
package com.edutech.center.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TeacherPendingApprovalEvent(
    UUID teacherId,
    UUID centerId,
    String centerName,
    String teacherFirstName,
    String teacherLastName,
    String teacherEmail,
    String subjects,
    Instant occurredAt
) {
    public TeacherPendingApprovalEvent(UUID teacherId, UUID centerId, String centerName,
                                        String firstName, String lastName,
                                        String email, String subjects) {
        this(teacherId, centerId, centerName, firstName, lastName, email, subjects, Instant.now());
    }
}
