package com.edutech.student.domain.event;

import com.edutech.student.domain.model.Stream;

import java.time.Instant;
import java.util.UUID;

public record StreamSelectedEvent(
        String eventId,
        UUID studentId,
        Stream stream,
        Instant occurredAt
) {}
