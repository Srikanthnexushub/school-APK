package com.edutech.aimentor.domain.port.in;

import com.edutech.aimentor.application.dto.DoubtTicketResponse;
import com.edutech.aimentor.application.dto.SubmitDoubtRequest;

public interface SubmitDoubtUseCase {

    DoubtTicketResponse submitDoubt(SubmitDoubtRequest request);
}
