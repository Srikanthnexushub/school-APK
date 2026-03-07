// src/main/java/com/edutech/center/domain/port/in/AssignTeacherUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AssignTeacherRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.TeacherResponse;

import java.util.UUID;

public interface AssignTeacherUseCase {
    TeacherResponse assignTeacher(UUID centerId, AssignTeacherRequest request, AuthPrincipal principal);
}
