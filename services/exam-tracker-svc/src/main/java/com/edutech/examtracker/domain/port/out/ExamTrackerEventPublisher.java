package com.edutech.examtracker.domain.port.out;

public interface ExamTrackerEventPublisher {

    void publish(Object event);
}
