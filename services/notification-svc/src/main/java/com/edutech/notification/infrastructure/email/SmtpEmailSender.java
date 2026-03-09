package com.edutech.notification.infrastructure.email;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.out.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP-backed email sender for the EMAIL notification channel.
 * In local dev, if SMTP is unavailable the send call will throw and the
 * {@code NotificationService} will mark the notification FAILED.
 */
@Component
public class SmtpEmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "recipientEmail is required for EMAIL channel, notificationId=" + notification.getId());
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject() != null ? notification.getSubject() : "(no subject)");
            helper.setText(notification.getBody(), false);
            mailSender.send(message);
            log.debug("EMAIL sent to={} notificationId={}", notification.getRecipientEmail(), notification.getId());
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException("SMTP delivery failed for notificationId=" + notification.getId()
                    + ": " + ex.getMessage(), ex);
        }
    }
}
