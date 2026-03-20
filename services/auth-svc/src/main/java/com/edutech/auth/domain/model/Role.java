// src/main/java/com/edutech/auth/domain/model/Role.java
package com.edutech.auth.domain.model;

/**
 * Role hierarchy (ordinal = seniority, lower ordinal = higher authority).
 * SUPER_ADMIN > INSTITUTION_ADMIN > CENTER_ADMIN > TEACHER > PARENT > STUDENT > GUEST
 */
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
