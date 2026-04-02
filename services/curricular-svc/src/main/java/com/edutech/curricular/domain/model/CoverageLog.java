package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.DeliveryMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "coverage_logs")
@Getter
@Setter
@NoArgsConstructor
public class CoverageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "taught_on", nullable = false)
    private LocalDate taughtOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 30)
    private DeliveryMethod deliveryMethod = DeliveryMethod.LECTURE;

    @Column(name = "duration_mins")
    private int durationMins = 60;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static CoverageLog create(UUID teacherId, UUID centerId, UUID topicId, LocalDate taughtOn, DeliveryMethod deliveryMethod, int durationMins, String notes) {
        CoverageLog cl = new CoverageLog();
        cl.teacherId = teacherId;
        cl.centerId = centerId;
        cl.topicId = topicId;
        cl.taughtOn = taughtOn;
        cl.deliveryMethod = deliveryMethod != null ? deliveryMethod : DeliveryMethod.LECTURE;
        cl.durationMins = durationMins > 0 ? durationMins : 60;
        cl.notes = notes;
        cl.createdAt = Instant.now();
        return cl;
    }
}
