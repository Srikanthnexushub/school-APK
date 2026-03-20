// src/main/java/com/edutech/parent/domain/model/Role.java
package com.edutech.parent.domain.model;

public enum Role {
    SUPER_ADMIN(7), INSTITUTION_ADMIN(6), CENTER_ADMIN(5), TEACHER(4), PARENT(3), STUDENT(2), GUEST(1);

    private final int rank;

    Role(int rank) {
        this.rank = rank;
    }

    public boolean hasHigherOrEqualRankThan(Role other) {
        return this.rank >= other.rank;
    }
}
