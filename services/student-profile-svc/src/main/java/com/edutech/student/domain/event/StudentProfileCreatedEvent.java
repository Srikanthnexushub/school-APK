package com.edutech.student.domain.event;

import com.edutech.student.domain.model.Stream;

import java.time.Instant;
import java.util.UUID;

public record StudentProfileCreatedEvent(
        String eventId,
        UUID studentId,
        UUID userId,
        String email,
        String city,
        String state,
        Stream stream,
        Instant occurredAt
) {}
