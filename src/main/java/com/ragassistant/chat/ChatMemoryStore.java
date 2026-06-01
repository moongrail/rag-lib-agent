package com.ragassistant.chat;

import dev.langchain4j.data.message.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatMemoryStore {

    private static final int MAX_MESSAGES = 20;

    private final ConcurrentHashMap<String, List<ChatMessage>> memories = new ConcurrentHashMap<>();

    public List<ChatMessage> history(String sessionId) {
        return new ArrayList<>(memories.getOrDefault(sessionId, new ArrayList<>()));
    }

    public void remember(String sessionId, ChatMessage... messages) {
        List<ChatMessage> list = memories.computeIfAbsent(sessionId, id -> new ArrayList<>());
        synchronized (list) {
            for (ChatMessage m : messages) {
                list.add(m);
            }
            while (list.size() > MAX_MESSAGES) {
                list.remove(0);
            }
        }
    }

    public void drop(String sessionId) {
        memories.remove(sessionId);
    }
}
