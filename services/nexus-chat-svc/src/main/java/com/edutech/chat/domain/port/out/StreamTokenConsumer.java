package com.edutech.chat.domain.port.out;

@FunctionalInterface
public interface StreamTokenConsumer {
    void onToken(String token);

    default void onComplete(int inputTokens, int outputTokens, int latencyMs) {}

    default void onError(Throwable t) {}
}
