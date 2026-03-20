// src/main/java/com/edutech/center/domain/model/JobType.java
package com.edutech.center.domain.model;

/**
 * Employment engagement type for a job posting.
 */
public enum JobType {

    FULL_TIME(
        "Full-Time",
        "Permanent, full-time position with standard working hours"
    ),
    PART_TIME(
        "Part-Time",
        "Part-time position with reduced hours; may be ongoing or fixed-term"
    ),
    CONTRACT(
        "Contract",
        "Fixed-term contract engagement for a defined project or academic period"
    );

    private final String displayName;
    private final String description;

    JobType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription()  { return description; }
}
