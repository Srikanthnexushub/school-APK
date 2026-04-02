package com.edutech.curricular.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "activity_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ActivitySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "venue", length = 200)
    private String venue;

    @Column(name = "duration_mins")
    private int durationMins = 60;

    @Column(name = "conducted_by_id")
    private UUID conductedById;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ActivitySession create(UUID activityId, LocalDate sessionDate, String venue, int durationMins, UUID conductedById, String notes) {
        ActivitySession as = new ActivitySession();
        as.activityId = activityId;
        as.sessionDate = sessionDate;
        as.venue = venue;
        as.durationMins = durationMins > 0 ? durationMins : 60;
        as.conductedById = conductedById;
        as.notes = notes;
        as.createdAt = Instant.now();
        return as;
    }
}
