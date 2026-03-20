// src/main/java/com/edutech/center/domain/model/Role.java
package com.edutech.center.domain.model;

public enum Role {
    SUPER_ADMIN,
    INSTITUTION_ADMIN,
    CENTER_ADMIN,
    TEACHER,
    PARENT,
    STUDENT,
    GUEST;

    public boolean hasHigherOrEqualRankThan(Role other) {
        return this.ordinal() <= other.ordinal();
    }
}
