// src/main/java/com/edutech/auth/domain/port/out/NotificationSender.java
package com.edutech.auth.domain.port.out;

public interface NotificationSender {
    void sendOtpEmail(String to, String otp, String purpose, int expiryMinutes);
    void sendOtpSms(String to, String otp, String purpose, int expiryMinutes);
}
