// src/main/java/com/edutech/parent/domain/model/StudentLink.java
package com.edutech.parent.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_links", schema = "parent_schema")
public class StudentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false, updatable = false)
    private UUID parentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkStatus status;

    @Column
    private String relationship;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "school_name")
    private String schoolName;

    @Column
    private String standard;

    @Column
    private String board;

    @Column(name = "roll_number")
    private String rollNumber;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private StudentLink() {}

    public static StudentLink create(UUID parentId, UUID studentId, String studentName, UUID centerId, String relationship) {
        StudentLink link = new StudentLink();
        link.parentId = parentId;
        link.studentId = studentId;
        link.studentName = studentName;
        link.centerId = centerId;
        link.relationship = (relationship != null && !relationship.isBlank()) ? relationship : "PARENT";
        link.status = LinkStatus.ACTIVE;
        link.createdAt = Instant.now();
        link.updatedAt = link.createdAt;
        return link;
    }

    public void revoke() {
        if (this.status == LinkStatus.REVOKED) {
            throw new IllegalStateException("Student link is already REVOKED: " + this.id);
        }
        this.status = LinkStatus.REVOKED;
        this.updatedAt = Instant.now();
    }

    public void updateChildDetails(LocalDate dob, String schoolName, String standard,
                                   String board, String rollNumber) {
        if (dob != null) {
            this.dateOfBirth = dob;
        }
        if (schoolName != null) {
            this.schoolName = schoolName;
        }
        if (standard != null) {
            this.standard = standard;
        }
        if (board != null) {
            this.board = board;
        }
        if (rollNumber != null) {
            this.rollNumber = rollNumber;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public UUID getCenterId() {
        return centerId;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public String getRelationship() {
        return relationship;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public String getStandard() {
        return standard;
    }

    public String getBoard() {
        return board;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
