package com.ragassistant.chat;

import dev.langchain4j.data.message.ChatMessage;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session history with TTL and caps.
 *
 * <p>Why TTL: the previous version stored every session forever —
 * a public bot leaks memory and grows without bound. 60-minute idle
 * expiry plus a cap on session count keeps a single instance safe.
 * Multi-instance deployments still need Redis/JDBC — documented in
 * {@code docs/REVIEW_2026.md} as P1, intentionally out of scope here
 * to stay backwards compatible.</p>
 */
@Component
public class ChatMemoryStore {

    static final int MAX_MESSAGES = 20;
    static final int MAX_SESSIONS = 2000;
    static final Duration TTL = Duration.ofMinutes(60);

    private final ConcurrentHashMap<String, Session> memories = new ConcurrentHashMap<>();

    public List<ChatMessage> history(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        Session s = memories.get(sessionId);
        if (s == null || s.expired()) {
            memories.remove(sessionId);
            return List.of();
        }
        synchronized (s.messages) {
            return new ArrayList<>(s.messages);
        }
    }

    public void remember(String sessionId, ChatMessage... messages) {
        if (sessionId == null || sessionId.isBlank() || messages == null) {
            return;
        }
        evictIfNeeded();
        Session s = memories.computeIfAbsent(sessionId, id -> new Session());
        synchronized (s.messages) {
            s.touched = Instant.now();
            for (ChatMessage m : messages) {
                if (m != null) {
                    s.messages.add(m);
                }
            }
            while (s.messages.size() > MAX_MESSAGES) {
                s.messages.remove(0);
            }
        }
    }

    public void drop(String sessionId) {
        if (sessionId != null) {
            memories.remove(sessionId);
        }
    }

    private void evictIfNeeded() {
        if (memories.size() < MAX_SESSIONS) {
            // Cheap lazy expiry on every ~256th write path is enough;
            // full scan only when approaching the cap.
            return;
        }
        Instant cutoff = Instant.now().minus(TTL);
        memories.entrySet().removeIf(e -> e.getValue().touched.isBefore(cutoff));
        if (memories.size() >= MAX_SESSIONS) {
            // Still over cap (clock skew / hot keys): drop oldest quarter.
            memories.entrySet().stream()
                    .sorted((a, b) -> a.getValue().touched.compareTo(b.getValue().touched))
                    .limit(memories.size() / 4 + 1)
                    .forEach(e -> memories.remove(e.getKey()));
        }
    }

    private static final class Session {
        final List<ChatMessage> messages = new ArrayList<>();
        volatile Instant touched = Instant.now();

        boolean expired() {
            return touched.plus(ChatMemoryStore.TTL).isBefore(Instant.now());
        }
    }
}
