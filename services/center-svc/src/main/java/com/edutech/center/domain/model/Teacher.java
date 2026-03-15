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

    @Column(name = "user_id")
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

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "invitation_token", length = 255)
    private String invitationToken;

    @Column(name = "invitation_token_expires_at")
    private Instant invitationTokenExpiresAt;

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
                    String lastName, String email, String phoneNumber,
                    String subjects, String district, String employeeId, TeacherStatus status) {
        this.id = id;
        this.centerId = centerId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.subjects = subjects;
        this.district = district;
        this.employeeId = employeeId;
        this.status = status;
        this.joinedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Standard assignment — user already exists in auth-svc. */
    public static Teacher create(UUID centerId, UUID userId, String firstName,
                                 String lastName, String email,
                                 String phoneNumber, String subjects) {
        return new Teacher(UUID.randomUUID(), centerId, userId,
                firstName, lastName, email, phoneNumber, subjects, null, null, TeacherStatus.ACTIVE);
    }

    /** Bulk-import stub — invitation sent, userId unknown until teacher accepts. */
    public static Teacher createInvitationStub(UUID centerId, String firstName, String lastName,
                                               String email, String phoneNumber, String subjects,
                                               String employeeId, String token, Instant tokenExpiry) {
        Teacher t = new Teacher(UUID.randomUUID(), centerId, null,
                firstName, lastName, email, phoneNumber, subjects, null, employeeId, TeacherStatus.INVITATION_SENT);
        t.invitationToken = token;
        t.invitationTokenExpiresAt = tokenExpiry;
        return t;
    }

    /** Self-registration — teacher registered independently, needs coordinator approval. */
    public static Teacher createPending(UUID centerId, UUID userId, String firstName,
                                        String lastName, String email, String phoneNumber,
                                        String subjects, String district) {
        return new Teacher(UUID.randomUUID(), centerId, userId,
                firstName, lastName, email, phoneNumber, subjects, district, null, TeacherStatus.PENDING_APPROVAL);
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

    /** Invitation accepted — link to auth-svc user and activate. */
    public void acceptInvitation(UUID userId) {
        if (this.status != TeacherStatus.INVITATION_SENT) {
            throw new IllegalStateException("Invitation already used or invalid");
        }
        this.userId = userId;
        this.status = TeacherStatus.ACTIVE;
        this.invitationToken = null;
        this.invitationTokenExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    /** Coordinator approves a self-registered teacher. */
    public void approve() {
        if (this.status != TeacherStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Teacher is not pending approval");
        }
        this.status = TeacherStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Coordinator rejects a self-registered teacher. */
    public void reject() {
        if (this.status != TeacherStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Teacher is not pending approval");
        }
        this.status = TeacherStatus.INACTIVE;
        this.deletedAt = Instant.now();
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
    public String getDistrict() { return district; }
    public String getEmployeeId() { return employeeId; }
    public String getInvitationToken() { return invitationToken; }
    public Instant getInvitationTokenExpiresAt() { return invitationTokenExpiresAt; }
    public TeacherStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
