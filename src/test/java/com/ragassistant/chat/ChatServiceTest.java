package com.ragassistant.chat;

import com.ragassistant.api.dto.ChatResponse;
import com.ragassistant.api.dto.Citation;
import com.ragassistant.retrieval.DocumentRetrievalService;
import com.ragassistant.retrieval.RetrievedChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private DocumentRetrievalService retrievalService;
    @Mock
    private ChatModel chatModel;
    @Mock
    private ChatMemoryStore memoryStore;

    @InjectMocks
    private ChatService service;

    private RetrievedChunk chunk(String fileName, String page, String text, double score) {
        Metadata metadata = new Metadata();
        metadata.put("fileName", fileName);
        if (page != null) {
            metadata.put("page", page);
        }
        return new RetrievedChunk(text, metadata, score);
    }

    @Test
    void ask_buildsPromptAnswersWithCitationsAndRemembers() {
        List<RetrievedChunk> chunks = List.of(
                chunk("doc1.txt", "2", "Relevant passage about topic one.", 0.91),
                chunk("doc2.txt", "", "Another passage without page.", 0.77));
        when(retrievalService.retrieve("acme", "What is X?")).thenReturn(chunks);
        when(memoryStore.history("s1")).thenReturn(List.of());
        when(chatModel.chat(anyList())).thenReturn(
                dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(AiMessage.from("Answer based on indexed docs")).build());

        ChatResponse response = service.ask("acme", "s1", "What is X?");

        assertThat(response.answer()).isEqualTo("Answer based on indexed docs");
        assertThat(response.sessionId()).isEqualTo("s1");
        assertThat(response.citations()).hasSize(2);
        Citation first = response.citations().get(0);
        assertThat(first.fileName()).isEqualTo("doc1.txt");
        assertThat(first.page()).isEqualTo(2);
        assertThat(first.score()).isEqualTo(0.91);

        ArgumentCaptor<List<dev.langchain4j.data.message.ChatMessage>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(chatModel).chat(captor.capture());
        String prompted = captor.getValue().stream()
                .filter(m -> m instanceof UserMessage)
                .map(m -> ((UserMessage) m).singleText())
                .findFirst().orElse("");
        assertThat(prompted).contains("What is X?").contains("Relevant passage");

        verify(memoryStore).remember(eq("s1"), any(UserMessage.class), any(AiMessage.class));
    }

    @Test
    void ask_generatesSessionIdWhenBlank() {
        when(retrievalService.retrieve(anyString(), anyString())).thenReturn(List.of(
                chunk("f.txt", null, "text", 0.5)));
        when(memoryStore.history(anyString())).thenReturn(List.of());
        when(chatModel.chat(anyList())).thenReturn(
                dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());

        ChatResponse response = service.ask("t", null, "q?");

        assertThat(response.sessionId()).isNotBlank();
    }

    @Test
    void ask_withNoChunksProducesEmptyContext() {
        when(retrievalService.retrieve("t", "q")).thenReturn(List.of());
        when(memoryStore.history("s")).thenReturn(List.of());
        when(chatModel.chat(anyList())).thenReturn(
                dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(AiMessage.from("no context answer")).build());

        ChatResponse response = service.ask("t", "s", "q");

        assertThat(response.answer()).isEqualTo("no context answer");
        assertThat(response.citations()).isEmpty();
    }

    @Test
    void ask_truncatesLongSnippet() {
        String longText = "x".repeat(500);
        when(retrievalService.retrieve(anyString(), anyString())).thenReturn(
                List.of(chunk("big.txt", "1", longText, 0.9)));
        when(memoryStore.history(anyString())).thenReturn(List.of());
        when(chatModel.chat(anyList())).thenReturn(
                dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(AiMessage.from("a")).build());

        Citation c = service.ask("t", "s", "q").citations().get(0);
        assertThat(c.snippet()).endsWith("...").hasSize(243);
    }
}
