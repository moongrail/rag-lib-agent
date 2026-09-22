package com.ragassistant.adapter.rest;

import com.ragassistant.common.exceptions.GlobalExceptionHandler;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentMetadataRepository;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.ingestion.DocumentIngestionService;
import com.ragassistant.storage.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private final DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
    private final DocumentMetadataRepository metadataRepository = mock(DocumentMetadataRepository.class);
    private final DocumentStorageService storageService = mock(DocumentStorageService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DocumentController controller = new DocumentController(ingestionService, metadataRepository, storageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_returnsOk() throws Exception {
        DocumentMetadata meta = new DocumentMetadata();
        meta.setId(UUID.randomUUID());
        meta.setFileName("doc.txt");
        meta.setStatus(DocumentStatus.INGESTED);
        when(ingestionService.ingest(any(MultipartFile.class), any(), any())).thenReturn(meta);

        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "hi".getBytes());
        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("doc.txt"))
                .andExpect(jsonPath("$.status").value("INGESTED"));
    }

    @Test
    void uploadBulk_returnsOk() throws Exception {
        DocumentMetadata meta = new DocumentMetadata();
        meta.setId(UUID.randomUUID());
        meta.setFileName("a.txt");
        meta.setStatus(DocumentStatus.INGESTED);
        when(ingestionService.ingestAll(any(), any(), any())).thenReturn(List.of(meta));

        MockMultipartFile f1 = new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes());
        mockMvc.perform(multipart("/api/documents/bulk").file(f1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("a.txt"));
    }

    @Test
    void list_returnsDocuments() throws Exception {
        DocumentMetadata meta = new DocumentMetadata();
        meta.setId(UUID.randomUUID());
        meta.setTitle("t");
        meta.setFileName("f.txt");
        meta.setStatus(DocumentStatus.INGESTED);
        meta.setChunkCount(3);
        when(metadataRepository.findByTenantId(any())).thenReturn(List.of(meta));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("f.txt"));
    }

    @Test
    void get_existing_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        DocumentMetadata meta = new DocumentMetadata();
        meta.setId(id);
        meta.setTitle("t");
        meta.setFileName("f.txt");
        meta.setStatus(DocumentStatus.INGESTED);
        meta.setChunkCount(1);
        when(metadataRepository.findByTenantIdAndId(any(), eq(id))).thenReturn(Optional.of(meta));

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("f.txt"));
    }

    @Test
    void get_missing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(metadataRepository.findByTenantIdAndId(any(), eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(status().isNotFound());
    }
}
