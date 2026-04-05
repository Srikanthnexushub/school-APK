package com.edutech.aigateway.infrastructure.external;

import com.edutech.aigateway.application.config.VoiceGatewayProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Sends a complete audio blob to Deepgram's pre-recorded REST endpoint and returns
 * the transcript.  Uses Nova-2 model with smart formatting and Indian-English locale.
 *
 * <p>Audio format accepted: {@code audio/webm;codecs=opus} — the native output of
 * browser {@code MediaRecorder}.  Deepgram Nova-2 handles this natively.</p>
 */
@Component
public class DeepgramSTTClient {

    private static final Logger log = LoggerFactory.getLogger(DeepgramSTTClient.class);
    private static final String DEEPGRAM_BASE = "https://api.deepgram.com";
    private static final String LISTEN_PATH   =
            "/v1/listen?model=nova-2&smart_format=true&language=en-IN&punctuate=true";

    private final WebClient webClient;
    private final VoiceGatewayProperties props;

    public DeepgramSTTClient(WebClient.Builder builder, VoiceGatewayProperties props) {
        this.props = props;
        this.webClient = builder
                .baseUrl(DEEPGRAM_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + props.deepgramApiKey())
                .build();
    }

    /**
     * Transcribes {@code audioBytes} (webm/opus) to text.
     *
     * @return transcript string, or empty string if nothing was detected
     */
    public Mono<String> transcribe(byte[] audioBytes, MediaType contentType) {
        if (props.isDeepgramPlaceholder()) {
            log.warn("Deepgram key not configured — returning echo transcript for dev");
            return Mono.just("[dev-mode: audio received, " + audioBytes.length + " bytes]");
        }
        if (audioBytes.length < 100) {
            return Mono.just("");
        }
        return webClient.post()
                .uri(LISTEN_PATH)
                .contentType(contentType)
                .bodyValue(audioBytes)
                .retrieve()
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(
                                        new RuntimeException("Deepgram error [" + resp.statusCode().value() + "]: " + err))))
                .bodyToMono(DeepgramResponse.class)
                .map(r -> {
                    try {
                        return r.results().channels().get(0).alternatives().get(0).transcript();
                    } catch (Exception e) {
                        return "";
                    }
                })
                .doOnSuccess(t -> log.info("Deepgram transcript: \"{}\"", t))
                .onErrorResume(e -> {
                    log.error("Deepgram STT failed: {}", e.getMessage());
                    return Mono.just("");
                });
    }

    // ── JSON response model ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepgramResponse(DeepgramResults results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepgramResults(List<DeepgramChannel> channels) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepgramChannel(List<DeepgramAlternative> alternatives) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepgramAlternative(String transcript, double confidence) {}
}
