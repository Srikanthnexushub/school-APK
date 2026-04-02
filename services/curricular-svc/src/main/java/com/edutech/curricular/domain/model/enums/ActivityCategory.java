package com.edutech.curricular.domain.model.enums;

public enum ActivityCategory {
    SPORTS("Sports"),
    ARTS("Arts"),
    MUSIC("Music"),
    DANCE("Dance"),
    DRAMA("Drama"),
    CODING_CLUB("Coding Club"),
    DEBATE("Debate"),
    SCIENCE_CLUB("Science Club"),
    COMMUNITY_SERVICE("Community Service"),
    LEADERSHIP("Leadership"),
    CULTURAL_EVENTS("Cultural Events"),
    OTHER("Other");

    private final String displayName;

    ActivityCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
