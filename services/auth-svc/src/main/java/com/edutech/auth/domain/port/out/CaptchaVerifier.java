// src/main/java/com/edutech/auth/domain/port/out/CaptchaVerifier.java
package com.edutech.auth.domain.port.out;

public interface CaptchaVerifier {
    boolean verify(String captchaToken, String ipAddress);
}
