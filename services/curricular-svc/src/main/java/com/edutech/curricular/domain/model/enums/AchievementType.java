package com.edutech.curricular.domain.model.enums;

public enum AchievementType {
    PARTICIPATION("Participation"),
    MERIT("Merit"),
    EXCELLENCE("Excellence"),
    GOLD("Gold"),
    SILVER("Silver"),
    BRONZE("Bronze"),
    SPECIAL("Special");

    private final String displayName;

    AchievementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
