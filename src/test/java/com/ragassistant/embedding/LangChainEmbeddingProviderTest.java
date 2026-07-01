package com.ragassistant.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChainEmbeddingProviderTest {

    private final EmbeddingModel model = mock(EmbeddingModel.class);
    private final LangChainEmbeddingProvider provider = new LangChainEmbeddingProvider(model, "local");

    @Test
    void embed_delegatesToModel() {
        when(model.embed("text")).thenReturn(Response.from(Embedding.from(new float[]{0.1f})));
        assertThat(provider.embed("text")).isInstanceOf(Embedding.class);
    }

    @Test
    void embedAll_delegatesToModel() {
        when(model.embedAll(anyList())).thenReturn(Response.from(List.of(Embedding.from(new float[]{1f}))));
        List<Embedding> result = provider.embedAll(List.of(TextSegment.from("x")));
        assertThat(result).hasSize(1);
    }

    @Test
    void dimensionAndName() {
        when(model.dimension()).thenReturn(384);
        assertThat(provider.dimension()).isEqualTo(384);
        assertThat(provider.providerName()).isEqualTo("local");
    }
}
