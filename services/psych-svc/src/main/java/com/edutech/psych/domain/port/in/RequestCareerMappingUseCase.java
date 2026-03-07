package com.edutech.psych.domain.port.in;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CareerMappingResponse;

import java.util.UUID;

public interface RequestCareerMappingUseCase {

    CareerMappingResponse requestCareerMapping(UUID profileId, AuthPrincipal principal);
}
