package com.edutech.psych.domain.port.out;

import java.util.UUID;

/**
 * Port for validating that a PARENT user is linked to a given student.
 * Implemented by the infrastructure adapter that calls parent-svc.
 */
public interface ParentStudentLinkValidationPort {

    /**
     * Returns true if an ACTIVE student-link exists in parent-svc for the given
     * parentUserId and studentId.
     *
     * Implementors must never throw — return false on any downstream failure so
     * that access is denied (fail-closed) rather than permitted.
     *
     * @param parentUserId the authenticated parent's user ID (from JWT)
     * @param studentId    the student whose profile is being requested
     * @return true iff the parent is actively linked to the student
     */
    boolean isParentLinkedToStudent(UUID parentUserId, UUID studentId);
}
