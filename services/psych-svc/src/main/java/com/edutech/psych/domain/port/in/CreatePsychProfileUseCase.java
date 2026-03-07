package com.edutech.psych.domain.port.in;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CreatePsychProfileRequest;
import com.edutech.psych.application.dto.PsychProfileResponse;

public interface CreatePsychProfileUseCase {

    PsychProfileResponse createProfile(CreatePsychProfileRequest req, AuthPrincipal principal);
}
