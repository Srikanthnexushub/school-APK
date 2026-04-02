package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "activity_enrollments")
@Getter
@Setter
@NoArgsConstructor
public class ActivityEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    public static ActivityEnrollment create(UUID activityId, UUID studentId, EnrollmentStatus status) {
        ActivityEnrollment ae = new ActivityEnrollment();
        ae.activityId = activityId;
        ae.studentId = studentId;
        ae.enrolledAt = Instant.now();
        ae.status = status != null ? status : EnrollmentStatus.ENROLLED;
        return ae;
    }

    public void withdraw() {
        this.status = EnrollmentStatus.WITHDRAWN;
        this.withdrawnAt = Instant.now();
    }

    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
    }
}
