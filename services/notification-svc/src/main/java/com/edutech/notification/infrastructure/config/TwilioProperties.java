package com.edutech.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio SMS gateway configuration.
 * In local/dev set accountSid to any value starting with "dev_" to enable
 * log-echo mode (messages are logged instead of actually sent via Twilio).
 */
@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String fromNumber
) {}
