// src/main/java/com/edutech/center/domain/model/StaffRoleType.java
package com.edutech.center.domain.model;

/**
 * Canonical role types for staff within a coaching center.
 * Each role carries a human-readable display name and a description
 * that drives the AI bio-generation prompt and the admin portal UI.
 */
public enum StaffRoleType {

    TEACHER(
        "Teacher",
        "Subject matter expert responsible for delivering curriculum content and student assessments"
    ),
    HOD(
        "Head of Department",
        "Departmental leadership, curriculum planning, and academic oversight of subject teachers"
    ),
    COORDINATOR(
        "Academic Coordinator",
        "Cross-departmental academic coordination, timetabling, and program scheduling"
    ),
    COUNSELOR(
        "Student Counselor",
        "Student welfare, career guidance, psychological support, and parent communication"
    ),
    LIBRARIAN(
        "Librarian",
        "Library operations, resource management, digital catalog maintenance, and reading programs"
    ),
    LAB_ASSISTANT(
        "Lab Assistant",
        "Laboratory setup, equipment maintenance, safety compliance, and practical session support"
    ),
    SPORTS_COACH(
        "Sports Coach",
        "Athletic training, physical development, inter-school event coordination, and fitness programs"
    ),
    ADMIN_STAFF(
        "Admin Staff",
        "Administrative operations, records management, fee processing, and general institutional support"
    );

    private final String displayName;
    private final String description;

    StaffRoleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription()  { return description; }
}
