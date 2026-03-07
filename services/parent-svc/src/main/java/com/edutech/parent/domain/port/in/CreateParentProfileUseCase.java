// src/main/java/com/edutech/parent/domain/port/in/CreateParentProfileUseCase.java
package com.edutech.parent.domain.port.in;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;

public interface CreateParentProfileUseCase {
    ParentProfileResponse createProfile(CreateParentProfileRequest request, AuthPrincipal principal);
}
