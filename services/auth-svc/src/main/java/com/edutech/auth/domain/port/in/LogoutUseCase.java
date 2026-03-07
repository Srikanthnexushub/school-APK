// src/main/java/com/edutech/auth/domain/port/in/LogoutUseCase.java
package com.edutech.auth.domain.port.in;

import java.util.UUID;

public interface LogoutUseCase {
    void logout(String refreshTokenId, UUID userId);
    void logoutAllDevices(UUID userId);
}
