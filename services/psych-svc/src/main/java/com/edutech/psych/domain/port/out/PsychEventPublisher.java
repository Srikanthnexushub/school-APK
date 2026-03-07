package com.edutech.psych.domain.port.out;

public interface PsychEventPublisher {

    void publish(Object event);
}
