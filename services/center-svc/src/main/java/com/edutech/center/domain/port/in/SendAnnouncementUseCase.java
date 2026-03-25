// src/main/java/com/edutech/center/domain/port/in/SendAnnouncementUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.command.SendAnnouncementCommand;
import com.edutech.center.application.dto.AnnouncementResponse;
import com.edutech.center.application.dto.AuthPrincipal;

import java.util.List;
import java.util.UUID;

public interface SendAnnouncementUseCase {
    AnnouncementResponse send(SendAnnouncementCommand command, AuthPrincipal principal);
    List<AnnouncementResponse> listByCenterId(UUID centerId, AuthPrincipal principal);
    void expire(UUID centerId, UUID announcementId, AuthPrincipal principal);
}
