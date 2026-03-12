package com.edutech.notification.infrastructure.sms;

import com.edutech.notification.domain.model.Notification;
import com.edutech.notification.domain.model.NotificationChannel;
import com.edutech.notification.domain.port.out.NotificationSender;
import com.edutech.notification.infrastructure.config.TwilioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sends SMS notifications via the Twilio Messaging REST API.
 *
 * <p><b>Dev / local-echo mode</b>: when {@code twilio.account-sid} starts with
 * {@code "dev_"} (the default placeholder in .env), no HTTP call is made —
 * the message is logged at INFO level instead. This lets the full notification
 * pipeline run end-to-end in local dev without real Twilio credentials.</p>
 *
 * <p><b>Production</b>: set real Twilio credentials in .env:
 * <pre>
 *   TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   TWILIO_FROM_NUMBER=+1XXXXXXXXXX
 * </pre>
 * </p>
 */
@Component
public class TwilioSmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);
    private static final String TWILIO_BASE_URL = "https://api.twilio.com/2010-04-01/Accounts/";

    private final TwilioProperties props;
    private final RestTemplate restTemplate;

    public TwilioSmsSender(TwilioProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) {
        String phone = notification.getRecipientPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException(
                    "recipientPhone is required for SMS channel, notificationId=" + notification.getId());
        }

        // Normalise to E.164: treat bare 10-digit Indian numbers
        String toNumber = normalisePhone(phone);
        String messageBody = buildSmsBody(notification);

        if (isDevMode()) {
            log.info("[SMS-DEV-ECHO] To={} From={} | Subject: {} | Body: {}",
                    toNumber, props.fromNumber(), notification.getSubject(), messageBody);
            return;
        }

        sendViaTwilio(toNumber, messageBody, notification.getId().toString());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private boolean isDevMode() {
        return props.accountSid() == null
                || props.accountSid().isBlank()
                || props.accountSid().startsWith("dev_")
                || props.accountSid().startsWith("ACdev");
    }

    /** Builds a concise SMS-friendly text (160 chars safe). */
    private String buildSmsBody(Notification n) {
        String subject = n.getSubject() != null ? n.getSubject() : "";
        String body    = n.getBody()    != null ? n.getBody()    : "";
        String combined = subject.isBlank() ? body : subject + ": " + body;
        // Truncate to 320 chars (2 SMS segments) — Twilio handles multi-part automatically
        return combined.length() > 320 ? combined.substring(0, 317) + "..." : combined;
    }

    /**
     * Normalises an Indian local number (10 digits starting with 6-9) to E.164 (+91...).
     * Already-formatted E.164 numbers are returned unchanged.
     */
    private String normalisePhone(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.length() == 10 && digits.matches("[6-9]\\d{9}")) {
            return "+91" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        }
        // Already has + or is some other format — return as-is
        return phone.startsWith("+") ? phone : "+" + phone;
    }

    private void sendViaTwilio(String to, String body, String notificationId) {
        String url = TWILIO_BASE_URL + props.accountSid() + "/Messages.json";

        String credentials = props.accountSid() + ":" + props.authToken();
        String authHeader  = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("From", props.fromNumber());
        params.add("To",   to);
        params.add("Body", body);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            log.debug("SMS sent via Twilio: to={} notificationId={}", to, notificationId);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Twilio SMS delivery failed for notificationId=" + notificationId
                            + " to=" + to + ": " + ex.getMessage(), ex);
        }
    }
}
