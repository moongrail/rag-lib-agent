package com.ragassistant.chat;

import com.ragassistant.api.dto.ChatResponse;
import com.ragassistant.api.dto.Citation;
import com.ragassistant.common.exceptions.RagException;
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

    // Caps protect the LLM context window and OpenRouter bill.
    // 5 chunks x ~1000 chars normally fits, but history + long chunks can overflow.
    static final int MAX_QUESTION_CHARS = 4000;
    static final int MAX_CHUNK_CHARS = 2000;
    static final int MAX_CONTEXT_CHARS = 12000;
    static final int SNIPPET_CHARS = 240;

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
        if (question == null || question.isBlank()) {
            throw new RagException("Question must not be blank");
        }
        String q = question.strip();
        if (q.length() > MAX_QUESTION_CHARS) {
            throw new RagException("Question too long (max " + MAX_QUESTION_CHARS + " characters)");
        }
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        List<RetrievedChunk> chunks = retrievalService.retrieve(tenantId, q);
        String context = buildContext(chunks);

        List<ChatMessage> history = memoryStore.history(sessionId);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.addAll(history);
        messages.add(UserMessage.from("Context:\n" + context + "\n\nQuestion: " + q));

        dev.langchain4j.model.chat.response.ChatResponse lcResponse = chatModel.chat(messages);
        AiMessage aiMessage = lcResponse.aiMessage();

        memoryStore.remember(sessionId, UserMessage.from(q), aiMessage);

        List<Citation> citations = chunks.stream().map(this::toCitation).toList();
        return new ChatResponse(sessionId, aiMessage.text(), citations);
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "NO CONTEXT FOUND in the indexed documents for this tenant.";
        }
        StringBuilder sb = new StringBuilder();
        for (RetrievedChunk chunk : chunks) {
            String text = chunk.text() == null ? "" : chunk.text().strip();
            if (text.length() > MAX_CHUNK_CHARS) {
                text = text.substring(0, text.offsetByCodePoints(0, MAX_CHUNK_CHARS)) + "...";
            }
            if (sb.length() + text.length() + 7 > MAX_CONTEXT_CHARS) {
                int room = MAX_CONTEXT_CHARS - sb.length() - 7;
                if (room > 200) {
                    sb.append(text, 0, text.offsetByCodePoints(0, room)).append("...");
                }
                break;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n---\n\n");
            }
            sb.append(text);
        }
        return sb.toString();
    }

    private Citation toCitation(RetrievedChunk chunk) {
        String fileName = chunk.metadata().getString("fileName");
        String pageStr = chunk.metadata().getString("page");
        Integer page = null;
        if (pageStr != null && !pageStr.isBlank()) {
            try {
                page = Integer.valueOf(pageStr.strip());
            } catch (NumberFormatException ignored) {
            }
        }
        String text = chunk.text() == null ? "" : chunk.text();
        String snippet = safeSnippet(text);
        return new Citation(fileName, page, snippet, chunk.score());
    }

    private String safeSnippet(String text) {
        String t = text.strip();
        int endIndex = t.offsetByCodePoints(0, Math.min(SNIPPET_CHARS, t.codePointCount(0, t.length())));
        if (t.length() <= endIndex) {
            return t;
        }
        return t.substring(0, endIndex) + "...";
    }
}
