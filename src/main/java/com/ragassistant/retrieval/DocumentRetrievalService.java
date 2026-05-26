package com.ragassistant.retrieval;

import com.ragassistant.config.AppProperties;
import com.ragassistant.embedding.EmbeddingProvider;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentRetrievalService {

    private final EmbeddingProvider embeddingProvider;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AppProperties appProperties;

    public DocumentRetrievalService(EmbeddingProvider embeddingProvider,
                                    EmbeddingStore<TextSegment> embeddingStore,
                                    AppProperties appProperties) {
        this.embeddingProvider = embeddingProvider;
        this.embeddingStore = embeddingStore;
        this.appProperties = appProperties;
    }

    public List<RetrievedChunk> retrieve(String tenantId, String query) {
        return retrieve(tenantId, query,
                appProperties.getRetrieval().getMaxResults(),
                appProperties.getRetrieval().getMinScore());
    }

    public List<RetrievedChunk> retrieve(String tenantId, String query, int maxResults, double minScore) {
        Embedding queryEmbedding = embeddingProvider.embed(query);
        var filter = MetadataFilterBuilder.metadataKey("tenantId").isEqualTo(tenantId);
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .query(query)
                .maxResults(maxResults)
                .minScore(minScore)
                .filter(filter)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches().stream()
                .map(this::toChunk)
                .toList();
    }

    private RetrievedChunk toChunk(EmbeddingMatch<TextSegment> match) {
        return new RetrievedChunk(
                match.embedded().text(),
                match.embedded().metadata(),
                match.score());
    }
}
