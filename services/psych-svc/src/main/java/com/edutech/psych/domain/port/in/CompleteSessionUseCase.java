package com.edutech.psych.domain.port.in;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CompleteSessionRequest;
import com.edutech.psych.application.dto.SessionResponse;

import java.util.UUID;

public interface CompleteSessionUseCase {

    SessionResponse completeSession(UUID sessionId, CompleteSessionRequest req, AuthPrincipal principal);
}
