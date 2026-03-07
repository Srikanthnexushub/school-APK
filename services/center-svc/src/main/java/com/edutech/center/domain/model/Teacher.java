// src/main/java/com/edutech/center/domain/model/Teacher.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Teacher profile within a center. Links a user (from auth-svc, via userId)
 * to a coaching center with subject expertise metadata.
 */
@Entity
@Table(name = "teachers", schema = "center_schema")
public class Teacher {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 500)
    private String subjects;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeacherStatus status;

    @Column(name = "joined_at", updatable = false, nullable = false)
    private Instant joinedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected Teacher() {}

    private Teacher(UUID id, UUID centerId, UUID userId, String firstName,
                    String lastName, String email, String phoneNumber, String subjects) {
        this.id = id;
        this.centerId = centerId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.subjects = subjects;
        this.status = TeacherStatus.ACTIVE;
        this.joinedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Teacher create(UUID centerId, UUID userId, String firstName,
                                 String lastName, String email,
                                 String phoneNumber, String subjects) {
        return new Teacher(UUID.randomUUID(), centerId, userId,
                firstName, lastName, email, phoneNumber, subjects);
    }

    public void updateSubjects(String subjects) {
        this.subjects = subjects;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = TeacherStatus.INACTIVE;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reactivate() {
        this.status = TeacherStatus.ACTIVE;
        this.deletedAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() { return this.status == TeacherStatus.ACTIVE && this.deletedAt == null; }

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public UUID getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getSubjects() { return subjects; }
    public TeacherStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
