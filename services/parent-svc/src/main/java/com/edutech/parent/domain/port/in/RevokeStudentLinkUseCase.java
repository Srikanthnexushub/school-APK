// src/main/java/com/edutech/parent/domain/port/in/RevokeStudentLinkUseCase.java
package com.edutech.parent.domain.port.in;

import com.edutech.parent.application.dto.AuthPrincipal;

import java.util.UUID;

public interface RevokeStudentLinkUseCase {
    void revokeLink(UUID linkId, AuthPrincipal principal);
}
