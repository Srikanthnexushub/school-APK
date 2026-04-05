package com.edutech.aigateway.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Voice AI gateway configuration — Deepgram STT + ElevenLabs TTS.
 * Bound from application.yml {@code voice.*} / .env VOICE_* keys.
 */
@ConfigurationProperties(prefix = "voice")
public record VoiceGatewayProperties(
        String deepgramApiKey,
        String elevenLabsApiKey,
        VoiceIds voiceIds
) {

    /** ElevenLabs voice IDs, one per persona. */
    public record VoiceIds(
            String neo,    // STUDENT  — Rachel
            String sage,   // TEACHER  — Daniel
            String aria,   // PARENT   — Elli
            String apex,   // CENTER_ADMIN — Josh
            String nexus   // INSTITUTION_ADMIN / SUPER_ADMIN — George
    ) {}

    public boolean isDeepgramPlaceholder() {
        return deepgramApiKey == null || deepgramApiKey.isBlank()
                || deepgramApiKey.startsWith("dg-dev");
    }

    public boolean isElevenLabsPlaceholder() {
        return elevenLabsApiKey == null || elevenLabsApiKey.isBlank()
                || elevenLabsApiKey.startsWith("sk-dev");
    }

    /** Returns the ElevenLabs voice ID for the given persona name. */
    public String voiceIdForPersona(String personaName) {
        if (voiceIds == null) return voiceIds().neo();
        return switch (personaName) {
            case "SAGE" -> voiceIds.sage();
            case "ARIA" -> voiceIds.aria();
            case "APEX" -> voiceIds.apex();
            case "NEXUS" -> voiceIds.nexus();
            default -> voiceIds.neo();
        };
    }
}
