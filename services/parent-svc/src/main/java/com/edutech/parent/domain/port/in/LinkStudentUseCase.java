// src/main/java/com/edutech/parent/domain/port/in/LinkStudentUseCase.java
package com.edutech.parent.domain.port.in;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.LinkStudentRequest;
import com.edutech.parent.application.dto.StudentLinkResponse;

import java.util.UUID;

public interface LinkStudentUseCase {
    StudentLinkResponse linkStudent(UUID parentProfileId, LinkStudentRequest request, AuthPrincipal principal);
}
