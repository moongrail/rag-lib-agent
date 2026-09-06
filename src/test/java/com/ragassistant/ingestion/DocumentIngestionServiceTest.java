package com.ragassistant.ingestion;

import com.ragassistant.config.AppProperties;
import com.ragassistant.common.exceptions.RagException;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentMetadataRepository;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.embedding.EmbeddingProvider;
import com.ragassistant.storage.DocumentStorageService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentIngestionServiceTest {

    @Mock
    private DocumentStorageService storageService;
    @Mock
    private EmbeddingProvider embeddingProvider;
    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;
    @Mock
    private DocumentMetadataRepository metadataRepository;
    @Mock
    private AppProperties appProperties;
    @Mock
    private DocumentParserFactory parserFactory;
    @Mock
    private DocumentParser documentParser;

    @InjectMocks
    private DocumentIngestionService service;

    private void stubBasics() {
        AppProperties.Ingestion ingestion = new AppProperties.Ingestion();
        ingestion.setChunkSize(100);
        ingestion.setChunkOverlap(20);
        when(appProperties.getIngestion()).thenReturn(ingestion);
        when(metadataRepository.save(any(DocumentMetadata.class))).thenAnswer(i -> {
            DocumentMetadata m = i.getArgument(0);
            if (m.getId() == null) {
                m.setId(UUID.randomUUID());
            }
            return m;
        });
        when(parserFactory.parserFor(any(), any())).thenReturn(documentParser);
        when(documentParser.parse(any())).thenReturn(Document.document("Some reasonably long text content that will be chunked and embedded for the test."));
        when(embeddingProvider.embedAll(anyList())).thenReturn(List.of(Embedding.from(new float[]{1f, 2f})));
    }

    @Test
    void ingest_success_storesEmbeddingsAndMarksIngested() {
        stubBasics();
        when(storageService.store(any(), any(), any())).thenReturn("/tmp/x.pdf");

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());
        DocumentMetadata result = service.ingest(file, "tenantA", "user");

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.INGESTED);
        assertThat(result.getChunkCount()).isGreaterThan(0);
        verify(embeddingStore, atLeastOnce()).addAll(anyList(), anyList());
        verify(embeddingProvider).embedAll(anyList());
    }

    @Test
    void ingest_failure_marksFailedAndDeletesStorage() {
        stubBasics();
        when(storageService.store(any(), any(), any())).thenReturn("/tmp/bad.pdf");
        when(documentParser.parse(any())).thenThrow(new RuntimeException("parse boom"));

        MultipartFile file = new MockMultipartFile("file", "bad.pdf", "application/pdf", "data".getBytes());
        assertThatThrownBy(() -> service.ingest(file, "tenantA", "user"))
                .isInstanceOf(RagException.class);
        verify(storageService).delete("/tmp/bad.pdf");
    }

    @Test
    void ingest_unsupportedFormat_marksFailedWithoutParsing() {
        stubBasics();
        MultipartFile file = new MockMultipartFile("file", "weird.xyz", "application/octet-stream", "data".getBytes());
        assertThatThrownBy(() -> service.ingest(file, "tenantA", "user"))
                .isInstanceOf(RagException.class);
        verifyNoInteractions(embeddingStore);
        verify(documentParser, never()).parse(any());
    }

    @Test
    void ingestAll_handlesMixedResults() {
        stubBasics();
        when(storageService.store(any(), any(), any())).thenReturn("/tmp/a.pdf", "/tmp/b.pdf");

        MultipartFile ok = new MockMultipartFile("file", "a.pdf", "application/pdf", "data".getBytes());
        MultipartFile bad = new MockMultipartFile("file", "bad.xyz", "application/octet-stream", "data".getBytes());
        List<DocumentMetadata> results = service.ingestAll(List.of(ok, bad), "tenantA", "user");

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(m -> m.getStatus() == DocumentStatus.INGESTED).count()).isEqualTo(1);
        assertThat(results.stream().filter(m -> m.getStatus() == DocumentStatus.FAILED).count()).isEqualTo(1);
    }
}
