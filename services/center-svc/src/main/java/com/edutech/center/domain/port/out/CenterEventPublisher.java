// src/main/java/com/edutech/center/domain/port/out/CenterEventPublisher.java
package com.edutech.center.domain.port.out;

public interface CenterEventPublisher {
    void publish(Object event);
}
