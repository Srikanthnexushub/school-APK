package com.edutech.student.domain.port.out;

public interface StudentEventPublisher {
    void publish(Object event);
}
