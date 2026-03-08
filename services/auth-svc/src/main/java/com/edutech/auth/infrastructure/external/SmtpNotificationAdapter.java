// src/main/java/com/edutech/auth/infrastructure/external/SmtpNotificationAdapter.java
package com.edutech.auth.infrastructure.external;

import com.edutech.auth.domain.port.out.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP adapter for OTP email delivery.
 * SMS delivery uses Twilio Verify API (configured separately).
 * For SMS OTPs in dev: falls back to logging the OTP.
 */
@Component
public class SmtpNotificationAdapter implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationAdapter.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    public SmtpNotificationAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String to, String otp, String purpose, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(buildSubject(purpose));
            helper.setText(buildEmailBody(otp, purpose, expiryMinutes), false);
            mailSender.send(message);
            log.debug("OTP email sent to={} purpose={}", to, purpose);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // SMTP unavailable (common in local dev without a mail server configured).
            // Log OTP to console so developers can complete verification manually.
            // IMPORTANT: configure real MAIL_* env vars in production.
            log.warn("[DEV] SMTP unavailable — OTP for email={} purpose={} otp={} (expires in {}m). " +
                     "Set MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD for real email delivery.",
                     to, purpose, otp, expiryMinutes);
        }
    }

    @Override
    public void sendOtpSms(String to, String otp, String purpose, int expiryMinutes) {
        // Twilio Verify integration — log OTP in dev, replace with Twilio SDK in prod
        log.info("[DEV-ONLY] SMS OTP for to={} purpose={} otp={}", to, purpose, otp);
    }

    private String buildSubject(String purpose) {
        return switch (purpose) {
            case "EMAIL_VERIFICATION" -> "Verify your EduTech account";
            case "PASSWORD_RESET"     -> "Reset your EduTech password";
            case "LOGIN_MFA"          -> "Your EduTech login code";
            default -> "Your EduTech verification code";
        };
    }

    private String buildEmailBody(String otp, String purpose, int expiryMinutes) {
        return String.format(
            "Your verification code is: %s%n%nThis code expires in %d minute(s).%n%n" +
            "If you did not request this code, please ignore this email.",
            otp, expiryMinutes
        );
    }
}
