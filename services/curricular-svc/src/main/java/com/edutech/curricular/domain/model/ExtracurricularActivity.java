package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.ActivityCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "extracurricular_activities")
@Getter
@Setter
@NoArgsConstructor
public class ExtracurricularActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ActivityCategory category;

    @Column(name = "max_participants")
    private int maxParticipants = 30;

    @Column(name = "supervisor_id")
    private UUID supervisorId;

    @Column(name = "schedule_description", length = 500)
    private String scheduleDescription;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static ExtracurricularActivity create(UUID centerId, String name, String description, ActivityCategory category, int maxParticipants, String scheduleDescription, UUID createdBy) {
        ExtracurricularActivity ea = new ExtracurricularActivity();
        ea.centerId = centerId;
        ea.name = name;
        ea.description = description;
        ea.category = category;
        ea.maxParticipants = maxParticipants > 0 ? maxParticipants : 30;
        ea.scheduleDescription = scheduleDescription;
        ea.createdBy = createdBy;
        ea.active = true;
        ea.createdAt = Instant.now();
        return ea;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
