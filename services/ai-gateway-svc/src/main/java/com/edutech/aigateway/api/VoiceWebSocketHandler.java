package com.edutech.aigateway.api;

import com.edutech.aigateway.application.config.VoiceGatewayProperties;
import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.VoicePersona;
import com.edutech.aigateway.domain.port.in.RouteCompletionUseCase;
import com.edutech.aigateway.infrastructure.external.DeepgramSTTClient;
import com.edutech.aigateway.infrastructure.external.ElevenLabsTTSClient;
import com.edutech.aigateway.infrastructure.security.JwtTokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enterprise Voice Agent WebSocket handler.
 *
 * <p><b>Client → Server protocol:</b></p>
 * <ul>
 *   <li>Connect: {@code ws://.../api/v1/ai/voice/ws?token={jwt}}</li>
 *   <li>Binary frame: raw webm/opus audio chunk (one or many while recording)</li>
 *   <li>Text JSON {@code {"type":"audio_end"}} — triggers STT→LLM→TTS pipeline</li>
 *   <li>Text JSON {@code {"type":"cancel"}} — discard accumulated audio</li>
 *   <li>Text JSON {@code {"type":"ping"}}</li>
 * </ul>
 *
 * <p><b>Server → Client protocol:</b></p>
 * <ul>
 *   <li>{@code {"type":"connected","persona":"NEO","tagline":"..."}}</li>
 *   <li>{@code {"type":"transcript","text":"..."}}</li>
 *   <li>{@code {"type":"llm_response","text":"..."}}</li>
 *   <li>Binary frames: MP3 chunks (ElevenLabs TTS output)</li>
 *   <li>{@code {"type":"tts_done"}}</li>
 *   <li>{@code {"type":"pong"}}</li>
 *   <li>{@code {"type":"error","message":"..."}}</li>
 * </ul>
 */
@Component
public class VoiceWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceWebSocketHandler.class);

    private final JwtTokenValidator jwtTokenValidator;
    private final RouteCompletionUseCase completionUseCase;
    private final DeepgramSTTClient deepgramClient;
    private final ElevenLabsTTSClient elevenLabsClient;
    private final VoiceGatewayProperties voiceProps;
    private final ObjectMapper objectMapper;

    public VoiceWebSocketHandler(JwtTokenValidator jwtTokenValidator,
                                 RouteCompletionUseCase completionUseCase,
                                 DeepgramSTTClient deepgramClient,
                                 ElevenLabsTTSClient elevenLabsClient,
                                 VoiceGatewayProperties voiceProps,
                                 ObjectMapper objectMapper) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.completionUseCase = completionUseCase;
        this.deepgramClient = deepgramClient;
        this.elevenLabsClient = elevenLabsClient;
        this.voiceProps = voiceProps;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // ── Auth: JWT from ?token= query param ───────────────────────────────
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractToken(query);
        AuthPrincipal principal;
        try {
            principal = jwtTokenValidator.validate(token);
        } catch (Exception e) {
            log.warn("Voice WS auth rejected: {}", e.getMessage());
            return session.close();
        }

        VoicePersona persona   = VoicePersona.fromRole(principal.role());
        String voiceId         = voiceProps.voiceIdForPersona(persona.displayName);
        log.info("Voice WS connected: userId={} persona={}", principal.userId(), persona.displayName);

        // ── Per-session audio accumulator ─────────────────────────────────────
        List<byte[]> audioChunks = new CopyOnWriteArrayList<>();

        // ── Output sink (unicast, back-pressure-buffered) ─────────────────────
        Sinks.Many<WebSocketMessage> sink = Sinks.many().unicast().onBackpressureBuffer();

        // Send connected banner
        emitJson(sink, session, Map.of(
                "type",    "connected",
                "persona", persona.displayName,
                "tagline", persona.tagline
        ));

        // ── Inbound ───────────────────────────────────────────────────────────
        Mono<Void> inbound = session.receive()
                .flatMap(msg -> dispatch(msg, audioChunks, sink, session, principal, persona, voiceId))
                .then()
                .doOnError(e -> {
                    log.error("Voice WS inbound error: {}", e.getMessage());
                    sink.tryEmitComplete();
                })
                .doOnTerminate(sink::tryEmitComplete);

        // ── Outbound ──────────────────────────────────────────────────────────
        Mono<Void> outbound = session.send(sink.asFlux());

        return Mono.zip(inbound, outbound).then();
    }

    // ── Message dispatch ──────────────────────────────────────────────────────

    private Mono<Void> dispatch(WebSocketMessage msg,
                                List<byte[]> audioChunks,
                                Sinks.Many<WebSocketMessage> sink,
                                WebSocketSession session,
                                AuthPrincipal principal,
                                VoicePersona persona,
                                String voiceId) {
        if (msg.getType() == WebSocketMessage.Type.BINARY) {
            byte[] bytes = new byte[msg.getPayload().readableByteCount()];
            msg.getPayload().read(bytes);
            audioChunks.add(bytes);
            return Mono.empty();
        }

        if (msg.getType() == WebSocketMessage.Type.TEXT) {
            String text = msg.getPayloadAsText();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(text, Map.class);
                return switch ((String) json.getOrDefault("type", "")) {
                    case "audio_end" -> runPipeline(audioChunks, sink, session, principal, persona, voiceId);
                    case "cancel"    -> { audioChunks.clear(); yield Mono.empty(); }
                    case "ping"      -> { emitJson(sink, session, Map.of("type", "pong")); yield Mono.empty(); }
                    default          -> Mono.empty();
                };
            } catch (Exception e) {
                log.debug("Voice WS: unparseable text frame, ignoring");
            }
        }
        return Mono.empty();
    }

    // ── STT → LLM → TTS pipeline ─────────────────────────────────────────────

    private Mono<Void> runPipeline(List<byte[]> audioChunks,
                                   Sinks.Many<WebSocketMessage> sink,
                                   WebSocketSession session,
                                   AuthPrincipal principal,
                                   VoicePersona persona,
                                   String voiceId) {
        List<byte[]> snapshot = new ArrayList<>(audioChunks);
        audioChunks.clear();

        if (snapshot.isEmpty()) return Mono.empty();

        byte[] audioBytes = concat(snapshot);

        // 1. Speech → Text
        return deepgramClient.transcribe(audioBytes, MediaType.parseMediaType("audio/webm;codecs=opus"))
                .flatMap(transcript -> {
                    if (transcript.isBlank()) {
                        emitJson(sink, session, Map.of("type", "transcript", "text", ""));
                        return Mono.empty();
                    }
                    emitJson(sink, session, Map.of("type", "transcript", "text", transcript));

                    // 2. Text → LLM
                    CompletionRequest req = new CompletionRequest(
                            principal.userId().toString(),
                            persona.systemPrompt,
                            transcript,
                            300,
                            0.7
                    );
                    return completionUseCase.routeCompletion(req, principal)
                            .flatMap(resp -> {
                                String llmText = resp.content() != null ? resp.content() : "";
                                if (llmText.isBlank()) return Mono.empty();

                                emitJson(sink, session, Map.of("type", "llm_response", "text", llmText));

                                // 3. Text → Speech
                                return streamTts(llmText, voiceId, sink, session);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Voice pipeline failed: {}", e.getMessage());
                    emitJson(sink, session, Map.of(
                            "type", "error",
                            "message", "Voice processing failed. Please try again."
                    ));
                    return Mono.empty();
                });
    }

    private Mono<Void> streamTts(String text,
                                 String voiceId,
                                 Sinks.Many<WebSocketMessage> sink,
                                 WebSocketSession session) {
        return elevenLabsClient.synthesize(text, voiceId)
                .doOnNext(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    sink.tryEmitNext(session.binaryMessage(f -> f.wrap(bytes)));
                })
                .then()
                .doOnSuccess(v -> emitJson(sink, session, Map.of("type", "tts_done")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void emitJson(Sinks.Many<WebSocketMessage> sink, WebSocketSession session, Map<String, Object> payload) {
        try {
            sink.tryEmitNext(session.textMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("emitJson failed: {}", e.getMessage());
        }
    }

    private static byte[] concat(List<byte[]> chunks) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (byte[] chunk : chunks) bos.write(chunk);
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static String extractToken(String query) {
        if (query == null) return "";
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) return param.substring(6);
        }
        return "";
    }
}
