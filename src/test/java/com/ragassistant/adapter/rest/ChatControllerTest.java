package com.ragassistant.adapter.rest;

import com.ragassistant.api.dto.ChatResponse;
import com.ragassistant.api.dto.Citation;
import com.ragassistant.chat.ChatService;
import com.ragassistant.common.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private final ChatService chatService = mock(ChatService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatController controller = new ChatController(chatService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void chat_returnsAnswer() throws Exception {
        ChatResponse response = new ChatResponse("s1", "the answer", List.of(new Citation("f.txt", 1, "snip", 0.9)));
        when(chatService.ask(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"sessionId\":\"s1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("the answer"))
                .andExpect(jsonPath("$.sessionId").value("s1"))
                .andExpect(jsonPath("$.citations[0].fileName").value("f.txt"));
    }

    @Test
    void chat_nullSession_returnsAnswer() throws Exception {
        ChatResponse response = new ChatResponse("gen", "ans", List.of());
        when(chatService.ask(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("ans"));
    }
}
