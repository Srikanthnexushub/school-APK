package com.edutech.performance.domain.port.out;

public interface PerformanceEventPublisher {

    void publish(Object event);
}
