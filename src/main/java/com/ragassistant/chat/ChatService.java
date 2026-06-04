package com.ragassistant.chat;

import com.ragassistant.api.dto.ChatResponse;
import com.ragassistant.api.dto.Citation;
import com.ragassistant.retrieval.DocumentRetrievalService;
import com.ragassistant.retrieval.RetrievedChunk;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are an enterprise research assistant. Answer the user's question strictly using the provided context.
            If the context does not contain the answer, say that you do not have enough information in the indexed documents.
            Cite the source file names used. Be concise, factual, and avoid hallucination.""";

    private final DocumentRetrievalService retrievalService;
    private final ChatModel chatModel;
    private final ChatMemoryStore memoryStore;

    public ChatService(DocumentRetrievalService retrievalService,
                       ChatModel chatModel,
                       ChatMemoryStore memoryStore) {
        this.retrievalService = retrievalService;
        this.chatModel = chatModel;
        this.memoryStore = memoryStore;
    }

    public ChatResponse ask(String tenantId, String sessionId, String question) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        List<RetrievedChunk> chunks = retrievalService.retrieve(tenantId, question);
        String context = chunks.stream()
                .map(RetrievedChunk::text)
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse("");

        List<ChatMessage> history = memoryStore.history(sessionId);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.addAll(history);
        messages.add(UserMessage.from("Context:\n" + context + "\n\nQuestion: " + question));

        dev.langchain4j.model.chat.response.ChatResponse lcResponse = chatModel.chat(messages);
        AiMessage aiMessage = lcResponse.aiMessage();

        memoryStore.remember(sessionId, UserMessage.from(question), aiMessage);

        List<Citation> citations = chunks.stream().map(this::toCitation).toList();
        return new ChatResponse(sessionId, aiMessage.text(), citations);
    }

    private Citation toCitation(RetrievedChunk chunk) {
        String fileName = chunk.metadata().getString("fileName");
        String pageStr = chunk.metadata().getString("page");
        Integer page = null;
        if (pageStr != null && !pageStr.isBlank()) {
            try {
                page = Integer.valueOf(pageStr);
            } catch (NumberFormatException ignored) {
            }
        }
        String snippet = chunk.text().length() <= 240 ? chunk.text() : chunk.text().substring(0, 240) + "...";
        return new Citation(fileName, page, snippet, chunk.score());
    }
}
