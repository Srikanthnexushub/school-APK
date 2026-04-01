package com.edutech.chat.domain.port.out;

import java.util.List;
import java.util.Map;

public interface AiGatewayStreamPort {
    /**
     * Streams completion tokens from the AI gateway.
     * Calls consumer.onToken() for each token, consumer.onComplete() when done,
     * consumer.onError() on failure.
     */
    void streamCompletion(String systemPrompt,
                          List<Map<String, String>> history,
                          String userMessage,
                          StreamTokenConsumer consumer);
}
