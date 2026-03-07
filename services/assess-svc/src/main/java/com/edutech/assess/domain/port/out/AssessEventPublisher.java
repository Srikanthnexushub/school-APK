// src/main/java/com/edutech/assess/domain/port/out/AssessEventPublisher.java
package com.edutech.assess.domain.port.out;

public interface AssessEventPublisher {
    void publish(Object event);
}
