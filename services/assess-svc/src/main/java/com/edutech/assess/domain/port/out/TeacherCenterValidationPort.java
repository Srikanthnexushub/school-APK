// src/main/java/com/edutech/assess/domain/port/out/TeacherCenterValidationPort.java
package com.edutech.assess.domain.port.out;

import java.util.UUID;

/**
 * Port for validating that a teacher belongs to a given center.
 * Used by GradeService when the teacher's centerId is null in the JWT
 * (happens until re-login after approval).
 */
public interface TeacherCenterValidationPort {
    /**
     * Returns true if the teacher identified by {@code teacherUserId} is
     * registered at {@code centerId} in the center service.
     * Implementations must be fail-closed (return false on any error).
     */
    boolean isTeacherAtCenter(UUID teacherUserId, UUID centerId);
}
