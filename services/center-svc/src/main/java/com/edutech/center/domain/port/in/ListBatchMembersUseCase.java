// src/main/java/com/edutech/center/domain/port/in/ListBatchMembersUseCase.java
package com.edutech.center.domain.port.in;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchMemberResponse;

import java.util.List;
import java.util.UUID;

public interface ListBatchMembersUseCase {
    List<BatchMemberResponse> listMembers(UUID batchId, AuthPrincipal principal);
}
