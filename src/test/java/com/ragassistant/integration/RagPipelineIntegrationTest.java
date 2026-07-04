package com.ragassistant.integration;

import com.ragassistant.config.AppProperties;
import com.ragassistant.config.VectorStoreConfig;
import com.ragassistant.domain.DocumentMetadata;
import com.ragassistant.domain.DocumentMetadataRepository;
import com.ragassistant.domain.DocumentStatus;
import com.ragassistant.embedding.EmbeddingProvider;
import com.ragassistant.embedding.LangChainEmbeddingProvider;
import com.ragassistant.ingestion.DocumentIngestionService;
import com.ragassistant.ingestion.DocumentParserFactory;
import com.ragassistant.chat.ChatService;
import com.ragassistant.retrieval.DocumentRetrievalService;
import com.ragassistant.storage.DocumentStorageService;
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.mock.web.MockMultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Full-pipeline integration test.
 *
 * It boots a real PostgreSQL + pgvector instance (the one started via docker-compose
 * on localhost:5432, user/password/db = rag/rag) and exercises the real
 * ingestion -> pgvector HYBRID retrieval -> chat flow with a deterministic stub
 * embedding model (no network, no model download) and a stubbed chat model.
 *
 * On an environment where Testcontainers is available you can swap the JDBC URL
 * for a containerised pgvector instance; the assertions below are provider-agnostic.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagPipelineIntegrationTest {

    private HikariDataSource dataSource;
    private DocumentIngestionService ingestionService;
    private DocumentRetrievalService retrievalService;
    private ChatService chatService;
    private EmbeddingStore<TextSegment> embeddingStore;

    static class StubEmbeddingModel implements EmbeddingModel {
        private final int dim = 384;

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> out = new ArrayList<>();
            for (TextSegment s : segments) {
                out.add(Embedding.from(vectorFor(s.text())));
            }
            return Response.from(out);
        }

        @Override
        public int dimension() {
            return dim;
        }

        private float[] vectorFor(String text) {
            float[] v = new float[dim];
            for (int i = 0; i < dim; i++) {
                v[i] = ((text.hashCode() * (i + 7)) % 1000) / 1000.0f;
            }
            return v;
        }
    }

    private static String jdbcUrl() {
        return System.getenv().getOrDefault("RAG_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/rag");
    }

    @BeforeAll
    void setUp() throws Exception {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl());
        dataSource.setUsername("rag");
        dataSource.setPassword("rag");

        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getClass().getResourceAsStream("/db/migration/V1__init.sql"), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            for (String stmt : sb.toString().split(";")) {
                if (!stmt.trim().isEmpty()) {
                    st.execute(stmt);
                }
            }
        }

        EmbeddingProvider provider = new LangChainEmbeddingProvider(new StubEmbeddingModel(), "stub");
        AppProperties props = new AppProperties();
        props.getIngestion().setChunkSize(200);
        props.getIngestion().setChunkOverlap(20);
        props.getRetrieval().setMaxResults(5);
        props.getRetrieval().setMinScore(0.0);
        props.getRetrieval().setRrfK(60);

        embeddingStore = new VectorStoreConfig().embeddingStore(dataSource, provider, props);
        DocumentStorageService storage = new DocumentStorageService(props);
        DocumentMetadataRepository repo = mock(DocumentMetadataRepository.class);
        when(repo.save(any(DocumentMetadata.class))).thenAnswer(inv -> {
            DocumentMetadata m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(UUID.randomUUID());
            }
            return m;
        });
        DocumentParserFactory parserFactory = new DocumentParserFactory();

        ingestionService = new DocumentIngestionService(storage, provider, embeddingStore, repo, props, parserFactory);
        retrievalService = new DocumentRetrievalService(provider, embeddingStore, props);

        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.chat(anyList())).thenReturn(
                dev.langchain4j.model.chat.response.ChatResponse.builder()
                        .aiMessage(dev.langchain4j.data.message.AiMessage.from("Stub answer from context"))
                        .build());
        chatService = new ChatService(retrievalService, chatModel, new com.ragassistant.chat.ChatMemoryStore());
    }

    @AfterAll
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void fullPipeline_ingestRetrieveChat() {
        String content = "Our refund policy allows returns within 30 days of purchase for unused items.";
        MockMultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain", content.getBytes());

        DocumentMetadata meta = ingestionService.ingest(file, "acme", "tester");
        assertThat(meta.getStatus()).isEqualTo(DocumentStatus.INGESTED);
        assertThat(meta.getChunkCount()).isGreaterThan(0);

        List<?> chunks = retrievalService.retrieve("acme", "refund policy");
        assertThat(chunks).isNotEmpty();

        var response = chatService.ask("acme", "sess1", "What is the refund policy?");
        assertThat(response.answer()).isEqualTo("Stub answer from context");
        assertThat(response.citations()).isNotEmpty();
    }

    @Test
    void tenantIsolation_noCrossTenantResults() {
        MockMultipartFile f = new MockMultipartFile("file", "a.txt", "text/plain",
                "Secret tenant-specific content.".getBytes());
        ingestionService.ingest(f, "tenantA", "tester");

        assertThat(retrievalService.retrieve("tenantB", "Secret")).isEmpty();
    }
}
