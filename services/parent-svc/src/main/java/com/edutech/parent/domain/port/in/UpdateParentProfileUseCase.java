// src/main/java/com/edutech/parent/domain/port/in/UpdateParentProfileUseCase.java
package com.edutech.parent.domain.port.in;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.UpdateParentProfileRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;

import java.util.UUID;

public interface UpdateParentProfileUseCase {
    ParentProfileResponse updateProfile(UUID profileId, UpdateParentProfileRequest request, AuthPrincipal principal);
}
