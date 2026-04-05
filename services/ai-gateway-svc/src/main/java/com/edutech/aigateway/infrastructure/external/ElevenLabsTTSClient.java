package com.edutech.aigateway.infrastructure.external;

import com.edutech.aigateway.application.config.VoiceGatewayProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Streams MP3 audio from ElevenLabs Turbo v2 TTS.
 *
 * <p>Returns a {@link Flux<DataBuffer>} of raw MP3 bytes — the caller is responsible
 * for draining and forwarding these to the WebSocket client.</p>
 */
@Component
public class ElevenLabsTTSClient {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsTTSClient.class);
    private static final String ELEVENLABS_BASE = "https://api.elevenlabs.io";

    private final WebClient webClient;
    private final VoiceGatewayProperties props;

    public ElevenLabsTTSClient(WebClient.Builder builder, VoiceGatewayProperties props) {
        this.props = props;
        this.webClient = builder
                .baseUrl(ELEVENLABS_BASE)
                .defaultHeader("xi-api-key", props.elevenLabsApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, "audio/mpeg")
                .build();
    }

    /**
     * Synthesizes {@code text} using the given {@code voiceId} and returns a stream of
     * MP3 {@link DataBuffer} chunks.
     */
    public Flux<DataBuffer> synthesize(String text, String voiceId) {
        if (props.isElevenLabsPlaceholder() || text.isBlank()) {
            log.warn("ElevenLabs key not configured or empty text — skipping TTS");
            return Flux.empty();
        }
        TtsRequest body = new TtsRequest(
                text,
                "eleven_turbo_v2_5",
                new VoiceSettings(0.45, 0.75, 0.0, false)
        );
        return webClient.post()
                .uri("/v1/text-to-speech/{voiceId}/stream", voiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> reactor.core.publisher.Mono.error(
                                        new RuntimeException("ElevenLabs TTS error [" + resp.statusCode().value() + "]: " + err))))
                .bodyToFlux(DataBuffer.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSubscribe(s -> log.info("ElevenLabs TTS streaming: voiceId={} textLen={}", voiceId, text.length()))
                .doOnError(e -> log.error("ElevenLabs TTS failed: {}", e.getMessage()))
                .onErrorResume(e -> Flux.empty());
    }

    // ── Request DTOs ──────────────────────────────────────────────────────────

    private record TtsRequest(
            String text,
            @JsonProperty("model_id") String modelId,
            @JsonProperty("voice_settings") VoiceSettings voiceSettings
    ) {}

    private record VoiceSettings(
            double stability,
            @JsonProperty("similarity_boost") double similarityBoost,
            double style,
            @JsonProperty("use_speaker_boost") boolean useSpeakerBoost
    ) {}
}
