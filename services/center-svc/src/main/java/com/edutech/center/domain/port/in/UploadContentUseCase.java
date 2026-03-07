// src/main/java/com/edutech/center/domain/port/in/UploadContentUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.UploadContentRequest;

import java.util.UUID;

public interface UploadContentUseCase {
    ContentItemResponse uploadContent(UUID centerId, UploadContentRequest request, AuthPrincipal principal);
}
