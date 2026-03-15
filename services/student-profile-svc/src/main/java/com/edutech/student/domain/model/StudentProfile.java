package com.edutech.student.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "students", schema = "student_schema")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column
    private String city;

    @Column
    private String state;

    @Column
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_board", nullable = false)
    private Board currentBoard;

    @Column(name = "current_class", nullable = false)
    private Integer currentClass;

    @Enumerated(EnumType.STRING)
    @Column
    private Stream stream;

    @Column(name = "target_year")
    private Integer targetYear;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subjects", columnDefinition = "JSONB")
    private List<String> subjects = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status;

    @Column(name = "parent_link_code", unique = true)
    private String parentLinkCode;

    @Column(name = "link_otp")
    private String linkOtp;

    @Column(name = "link_otp_expires_at")
    private Instant linkOtpExpiresAt;

    @Column(name = "link_otp_parent_user_id")
    private UUID linkOtpParentUserId;

    @Column(name = "link_otp_parent_name")
    private String linkOtpParentName;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private StudentProfile() {}

    public static StudentProfile create(UUID userId,
                                        String firstName,
                                        String lastName,
                                        String email,
                                        String phone,
                                        Gender gender,
                                        LocalDate dateOfBirth,
                                        String city,
                                        String state,
                                        String pincode,
                                        Board board,
                                        Integer currentClass) {
        StudentProfile profile = new StudentProfile();
        profile.userId = userId;
        profile.firstName = firstName;
        profile.lastName = lastName;
        profile.email = email;
        profile.phone = phone;
        profile.gender = gender;
        profile.dateOfBirth = dateOfBirth;
        profile.city = city;
        profile.state = state;
        profile.pincode = pincode;
        profile.currentBoard = board;
        profile.currentClass = currentClass;
        profile.status = ProfileStatus.ACTIVE;
        profile.parentLinkCode = generateCode();
        profile.createdAt = Instant.now();
        profile.updatedAt = profile.createdAt;
        return profile;
    }

    public void regenerateLinkCode() {
        this.parentLinkCode = generateCode();
        this.updatedAt = Instant.now();
    }

    private static String generateCode() {
        int code = (int) (Math.random() * 1_000_000);
        return String.format("%06d", code);
    }

    public void updateName(String firstName, String lastName) {
        if (firstName != null && !firstName.isBlank()) this.firstName = firstName;
        if (lastName != null && !lastName.isBlank()) this.lastName = lastName;
        this.updatedAt = Instant.now();
    }

    public void updatePhone(String phone) {
        if (phone != null) {
            this.phone = phone;
        }
        this.updatedAt = Instant.now();
    }

    public void updateGender(Gender gender) {
        if (gender != null) {
            this.gender = gender;
        }
        this.updatedAt = Instant.now();
    }

    public void updateLocation(String city, String state) {
        if (city != null) {
            this.city = city;
        }
        if (state != null) {
            this.state = state;
        }
        this.updatedAt = Instant.now();
    }

    public void selectStream(Stream stream) {
        this.stream = stream;
        this.updatedAt = Instant.now();
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = ProfileStatus.INACTIVE;
        this.updatedAt = this.deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public Board getCurrentBoard() {
        return currentBoard;
    }

    public Integer getCurrentClass() {
        return currentClass;
    }

    public Stream getStream() {
        return stream;
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public ProfileStatus getStatus() {
        return status;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects != null ? subjects : new ArrayList<>();
    }

    public String getParentLinkCode() {
        return parentLinkCode;
    }

    public void generateLinkOtp(UUID parentUserId, String parentName) {
        int code = (int) (Math.random() * 1_000_000);
        this.linkOtp = String.format("%06d", code);
        this.linkOtpExpiresAt = Instant.now().plusSeconds(300);
        this.linkOtpParentUserId = parentUserId;
        this.linkOtpParentName = parentName;
        this.updatedAt = Instant.now();
    }

    public boolean isLinkOtpValid() {
        return linkOtp != null && linkOtpExpiresAt != null && Instant.now().isBefore(linkOtpExpiresAt);
    }

    public void clearLinkOtp() {
        this.linkOtp = null;
        this.linkOtpExpiresAt = null;
        this.linkOtpParentUserId = null;
        this.linkOtpParentName = null;
        this.updatedAt = Instant.now();
    }

    public String getLinkOtp() {
        return linkOtp;
    }

    public Instant getLinkOtpExpiresAt() {
        return linkOtpExpiresAt;
    }

    public String getLinkOtpParentName() {
        return linkOtpParentName;
    }
}
