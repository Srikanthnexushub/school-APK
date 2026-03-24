package com.edutech.assess.infrastructure.websocket;

import com.edutech.assess.infrastructure.security.JwtTokenValidator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates JWT on every STOMP CONNECT frame.
 * Unauthenticated CONNECT frames are rejected with an AccessDeniedException.
 * This is the correct authentication layer for STOMP-over-WebSocket —
 * HTTP filter-level auth cannot cover WebSocket upgrade adequately.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenValidator jwtTokenValidator;

    public StompAuthChannelInterceptor(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException(
                    "Missing or invalid Authorization header on STOMP CONNECT");
            }
            String token = authHeader.substring(7);
            var principal = jwtTokenValidator.validate(token)
                .orElseThrow(() -> new AccessDeniedException("Invalid or expired JWT on STOMP CONNECT"));
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            accessor.setUser(auth);
        }
        return message;
    }
}
