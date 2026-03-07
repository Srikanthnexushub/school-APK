package com.edutech.aimentor.domain.port.out;

import com.edutech.aimentor.domain.model.SubjectArea;

public interface AiGatewayClient {

    /**
     * Sends a doubt question to the AI gateway and returns the AI-generated answer.
     *
     * Implementations must handle failures gracefully — if the AI call fails,
     * the implementation should throw an exception so the caller can apply
     * graceful degradation (e.g., keep the ticket in PENDING state).
     *
     * @param question    the student's question text
     * @param subjectArea the subject area for context
     * @return the AI-generated answer string
     */
    String resolveDoubt(String question, SubjectArea subjectArea);
}
