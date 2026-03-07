package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.DoubtTicketResponse;

import java.util.UUID;

public interface GetDoubtUseCase {

    DoubtTicketResponse getDoubt(UUID doubtTicketId, UUID studentId);
}
