package com.ragassistant.adapter.webui;

import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.ingestion.DocumentIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ChatWebControllerTest {

    private final DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatWebController(ingestionService)).build();
    }

    @Test
    void index_returnsChatView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"));
    }

    @Test
    void upload_success_setsFlashMessage() throws Exception {
        DocumentMetadata meta = new DocumentMetadata();
        meta.setStatus(DocumentStatus.INGESTED);
        when(ingestionService.ingestAll(anyList(), any(), any())).thenReturn(List.of(meta));

        MockMultipartFile f = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        mockMvc.perform(multipart("/ui/documents").file(f))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    void upload_error_setsFlashError() throws Exception {
        when(ingestionService.ingestAll(anyList(), any(), any())).thenThrow(new RuntimeException("boom"));

        MockMultipartFile f = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        mockMvc.perform(multipart("/ui/documents").file(f))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}
