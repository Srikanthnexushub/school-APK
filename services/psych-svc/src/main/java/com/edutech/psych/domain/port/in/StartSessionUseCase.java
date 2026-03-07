package com.edutech.psych.domain.port.in;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.SessionResponse;
import com.edutech.psych.application.dto.StartSessionRequest;

import java.util.UUID;

public interface StartSessionUseCase {

    SessionResponse startSession(UUID profileId, StartSessionRequest req, AuthPrincipal principal);
}
