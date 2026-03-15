// src/main/java/com/edutech/center/domain/event/TeacherInvitationEvent.java
package com.edutech.center.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TeacherInvitationEvent(
    UUID teacherId,
    UUID centerId,
    String centerName,
    String email,
    String firstName,
    String lastName,
    String invitationToken,
    Instant occurredAt
) {
    public TeacherInvitationEvent(UUID teacherId, UUID centerId, String centerName,
                                   String email, String firstName, String lastName,
                                   String invitationToken) {
        this(teacherId, centerId, centerName, email, firstName, lastName,
             invitationToken, Instant.now());
    }
}
