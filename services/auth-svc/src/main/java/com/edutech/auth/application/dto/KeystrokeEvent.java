// src/main/java/com/edutech/auth/application/dto/KeystrokeEvent.java
package com.edutech.auth.application.dto;

public record KeystrokeEvent(
    long keyDownTime,
    long keyUpTime,
    String key
) {
    public long holdDuration() {
        return keyUpTime - keyDownTime;
    }
}
