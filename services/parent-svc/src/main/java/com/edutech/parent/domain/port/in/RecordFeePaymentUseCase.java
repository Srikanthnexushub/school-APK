// src/main/java/com/edutech/parent/domain/port/in/RecordFeePaymentUseCase.java
package com.edutech.parent.domain.port.in;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.FeePaymentResponse;
import com.edutech.parent.application.dto.RecordFeePaymentRequest;

import java.util.UUID;

public interface RecordFeePaymentUseCase {
    FeePaymentResponse recordPayment(UUID parentProfileId, RecordFeePaymentRequest request, AuthPrincipal principal);
}
