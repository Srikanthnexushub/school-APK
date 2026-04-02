package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.AchievementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "activity_achievements")
@Getter
@Setter
@NoArgsConstructor
public class ActivityAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", nullable = false, length = 30)
    private AchievementType achievementType;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "issued_by_id", nullable = false)
    private UUID issuedById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ActivityAchievement create(UUID studentId, UUID activityId, AchievementType achievementType, String title, String description, LocalDate issuedAt, UUID issuedById) {
        ActivityAchievement aa = new ActivityAchievement();
        aa.studentId = studentId;
        aa.activityId = activityId;
        aa.achievementType = achievementType;
        aa.title = title;
        aa.description = description;
        aa.issuedAt = issuedAt;
        aa.issuedById = issuedById;
        aa.createdAt = Instant.now();
        return aa;
    }
}
