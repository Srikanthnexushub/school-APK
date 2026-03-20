// src/main/java/com/edutech/center/domain/model/JobPostingStatus.java
package com.edutech.center.domain.model;

/**
 * Lifecycle states for a job posting.
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>DRAFT  → OPEN    (publish)</li>
 *   <li>OPEN   → CLOSED  (close — no longer accepting applications)</li>
 *   <li>OPEN   → FILLED  (position filled)</li>
 *   <li>CLOSED → DRAFT   (toDraft — re-open for editing)</li>
 *   <li>FILLED → DRAFT   (toDraft — re-open for editing)</li>
 * </ul>
 */
public enum JobPostingStatus {

    DRAFT(
        "Draft",
        "Job posting is saved but not yet visible on the public job board"
    ),
    OPEN(
        "Open",
        "Job posting is active and visible on the public job board; accepting applications"
    ),
    CLOSED(
        "Closed",
        "Job posting has been closed; no longer accepting new applications"
    ),
    FILLED(
        "Filled",
        "Position has been filled; posting is retained for audit purposes"
    );

    private final String displayName;
    private final String description;

    JobPostingStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription()  { return description; }
}
