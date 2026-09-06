package com.ragassistant.ingestion;

import com.ragassistant.common.exceptions.RagException;
import com.ragassistant.config.AppProperties;
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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceEdgeTest {

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
        when(storageService.store(any(), any(), any())).thenReturn("/tmp/x.pdf");
        when(parserFactory.parserFor(any(), any())).thenReturn(documentParser);
        when(documentParser.parse(any())).thenReturn(Document.document("Reasonably long text content to be chunked and embedded."));
        when(embeddingProvider.embedAll(anyList())).thenReturn(List.of(Embedding.from(new float[]{1f, 2f})));
    }

    @Test
    void ingestPath_success(@TempDir Path dir) throws Exception {
        stubBasics();
        Path file = dir.resolve("doc.txt");
        Files.writeString(file, "Some document content for path ingestion.");

        DocumentMetadata result = service.ingest(file, "tenantA", "user");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.INGESTED);
        assertThat(result.getChunkCount()).isGreaterThan(0);
    }

    @Test
    void ingestPath_detectsContentTypeForXml(@TempDir Path dir) throws Exception {
        stubBasics();
        Path file = dir.resolve("data.xml");
        Files.writeString(file, "<root><child>xml content</child></root>");

        DocumentMetadata result = service.ingest(file, "tenantA", "user");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.INGESTED);
    }

    @Test
    void ingestPath_missingFile_throwsRagException() {
        assertThatThrownBy(() -> service.ingest(Path.of("no", "such", "file.txt"), "t", "u"))
                .isInstanceOf(RagException.class)
                .hasMessageContaining("Failed to read file");
    }

    @Test
    void ingestAllPaths_success(@TempDir Path dir) throws Exception {
        stubBasics();
        Path f1 = dir.resolve("a.txt");
        Path f2 = dir.resolve("b.md");
        Files.writeString(f1, "alpha content");
        Files.writeString(f2, "beta content");

        List<DocumentMetadata> results = service.ingestAllPaths(List.of(f1, f2), "tenantA", "user");
        assertThat(results).hasSize(2);
        assertThat(results.stream().allMatch(m -> m.getStatus() == DocumentStatus.INGESTED)).isTrue();
    }

    @Test
    void ingestAllPaths_failureRecorded(@TempDir Path dir) throws Exception {
        stubBasics();
        Path ok = dir.resolve("a.txt");
        Files.writeString(ok, "alpha content");
        Path missing = Path.of("no", "such", "missing.txt");

        List<DocumentMetadata> results = service.ingestAllPaths(List.of(ok, missing), "tenantA", "user");
        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(m -> m.getStatus() == DocumentStatus.FAILED).count()).isEqualTo(1);
    }
}
