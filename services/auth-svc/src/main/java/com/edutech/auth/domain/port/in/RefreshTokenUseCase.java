// src/main/java/com/edutech/auth/domain/port/in/RefreshTokenUseCase.java
package com.edutech.auth.domain.port.in;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.TokenPair;

public interface RefreshTokenUseCase {
    TokenPair refresh(String refreshTokenId, DeviceFingerprint deviceFingerprint);
}
