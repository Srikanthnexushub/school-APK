// src/main/java/com/edutech/auth/domain/port/in/AuthenticateUserUseCase.java
package com.edutech.auth.domain.port.in;

import com.edutech.auth.application.dto.LoginRequest;
import com.edutech.auth.application.dto.TokenPair;

public interface AuthenticateUserUseCase {
    TokenPair authenticate(LoginRequest request, String ipAddress, String userAgent);
}
