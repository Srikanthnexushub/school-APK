package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.DoubtTicketResponse;

import java.util.List;
import java.util.UUID;

public interface ListDoubtsUseCase {

    List<DoubtTicketResponse> listDoubts(UUID studentId);
}
