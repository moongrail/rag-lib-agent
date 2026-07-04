package com.ragassistant.retrieval;

import com.ragassistant.config.AppProperties;
import com.ragassistant.embedding.EmbeddingProvider;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentRetrievalServiceTest {

    @Mock
    private EmbeddingProvider embeddingProvider;
    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    private DocumentRetrievalService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getRetrieval().setMaxResults(5);
        props.getRetrieval().setMinScore(0.0);
        props.getRetrieval().setRrfK(60);
        service = new DocumentRetrievalService(embeddingProvider, embeddingStore, props);

        when(embeddingProvider.embed(anyString())).thenReturn(Embedding.from(new float[384]));

        TextSegment seg = TextSegment.from("Context passage", Metadata.from("fileName", "doc.txt"));
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.031, "id1", Embedding.from(new float[384]), seg);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));
    }

    @Test
    void retrieve_buildsHybridRequestWithTenantFilter() {
        List<RetrievedChunk> chunks = service.retrieve("acme", "question?");

        ArgumentCaptor<EmbeddingSearchRequest> captor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());
        EmbeddingSearchRequest req = captor.getValue();
        assertThat(req.query()).contains("question?");
        assertThat(req.maxResults()).isEqualTo(5);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("Context passage");
        assertThat(chunks.get(0).metadata().getString("fileName")).isEqualTo("doc.txt");
    }

    @Test
    void retrieve_usesDefaultsFromProperties() {
        List<RetrievedChunk> chunks = service.retrieve("acme", "q");
        assertThat(chunks).hasSize(1);
    }
}
