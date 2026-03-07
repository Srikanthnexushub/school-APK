package com.edutech.aigateway.domain.port.out;

public interface AiGatewayEventPublisher {
    // best-effort, void (not reactive — fire and forget)
    void publish(Object event);
}
