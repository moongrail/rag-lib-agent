package com.ragassistant.config;

import com.ragassistant.embedding.EmbeddingProvider;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class VectorStoreConfig {

    @Bean
    public EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore(
            DataSource dataSource,
            EmbeddingProvider embeddingProvider,
            AppProperties props) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("rag_embeddings")
                .dimension(embeddingProvider.dimension())
                .searchMode(PgVectorEmbeddingStore.SearchMode.HYBRID)
                .textSearchConfig("simple")
                .rrfK(props.getRetrieval().getRrfK())
                .createTable(true)
                .build();
    }
}
