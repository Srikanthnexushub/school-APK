package com.edutech.chat.domain.port.in;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import java.util.UUID;

public interface StreamMessageUseCase {
    ResponseBodyEmitter streamMessage(UUID sessionId, UUID userId, String userMessage,
                                      String pageContext, String jwtToken);
}
