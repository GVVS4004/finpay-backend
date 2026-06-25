package com.finpay.notification.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class SseService implements MessageListener {
    private final Map<Long, SseEmitterWithExpiry> emitters = new ConcurrentHashMap<>();
    public SseEmitter subscribe(Long userId, long tokenExpiry) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        emitters.put(userId, new SseEmitterWithExpiry(emitter, tokenExpiry));
        log.info("User {} subscribed to SSE, token expires at {}", userId, tokenExpiry);
        return emitter;
    }
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String payload = new String(message.getBody());

        Long userId = extractUserId(channel);
        if (userId == null) return;

        SseEmitterWithExpiry emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.getEmitter().send(SseEmitter.event()
                    .name("notification")
                    .data(payload));
            log.info("SSE event sent to user {}", userId);
        } catch (IOException e) {
            log.warn("Failed to send SSE to user {}, removing emitter", userId);
            emitters.remove(userId);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void closeExpiredConnections() {
        long now = System.currentTimeMillis();
        emitters.forEach((userId, entry) -> {
            if (entry.isExpired(now)) {
                log.info("Token expired for user {}, closing SSE connection", userId);
                entry.getEmitter().complete();
                emitters.remove(userId);
            }
        });
    }
    private Long extractUserId(String channel){
        try {
            return Long.parseLong(channel.replace("notification:",""));
        }catch (NumberFormatException e){
            return null;
        }
    }

    private static class SseEmitterWithExpiry{
        private final SseEmitter emitter;
        private final long expiryMs;

        SseEmitterWithExpiry(SseEmitter emitter, long expiryMs){
            this.emitter = emitter;
            this.expiryMs = expiryMs;
        }
        SseEmitter getEmitter() {return emitter;}
        boolean isExpired(long now) { return now>= expiryMs;}
    }
}
