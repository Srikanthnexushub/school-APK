// src/main/java/com/edutech/auth/domain/port/in/RegisterUserUseCase.java
package com.edutech.auth.domain.port.in;

import com.edutech.auth.application.dto.RegisterRequest;
import com.edutech.auth.application.dto.TokenPair;

public interface RegisterUserUseCase {
    TokenPair register(RegisterRequest request, String ipAddress, String userAgent);
}
