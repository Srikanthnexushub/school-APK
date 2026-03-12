package com.edutech.notification.infrastructure.sse;

import com.edutech.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of active SSE connections keyed by userId.
 * Supports multiple concurrent browser tabs per user.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Registers a new SSE emitter for the given user and returns it.
     * The caller should return this emitter as the HTTP response body.
     */
    public SseEmitter subscribe(UUID userId) {
        // Long.MAX_VALUE = connection never times out on server side; browser reconnects on network drop
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        List<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(()    -> remove(userId, emitter));
        emitter.onError(ex      -> remove(userId, emitter));

        // Send a heartbeat immediately so the browser knows the connection is live
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ex) {
            remove(userId, emitter);
        }

        log.debug("SSE subscriber registered: userId={} total={}", userId, userEmitters.size());
        return emitter;
    }

    /**
     * Pushes a new IN_APP notification to all active connections for the recipient.
     * Silently removes dead emitters.
     */
    public void push(Notification notification) {
        UUID userId = notification.getRecipientId();
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No active SSE connections for userId={} — notification stored for polling", userId);
            return;
        }

        Map<String, Object> payload = buildPayload(notification);
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(notification.getId().toString())
                        .name("notification")
                        .data(payload));
            } catch (IOException ex) {
                log.debug("Dead SSE emitter for userId={} — removing", userId);
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
    }

    private void remove(UUID userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            log.debug("SSE emitter removed: userId={} remaining={}", userId, list.size());
        }
    }

    private Map<String, Object> buildPayload(Notification n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",               n.getId());
        map.put("subject",          n.getSubject());
        map.put("body",             n.getBody());
        map.put("notificationType", n.getNotificationType());
        map.put("actionUrl",        n.getActionUrl());
        map.put("channel",          n.getChannel().name());
        map.put("createdAt",        n.getCreatedAt());
        map.put("readAt",           n.getReadAt());
        return map;
    }
}
