package com.ragassistant.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

public class LangChainEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;
    private final String providerName;

    public LangChainEmbeddingProvider(EmbeddingModel embeddingModel, String providerName) {
        this.embeddingModel = embeddingModel;
        this.providerName = providerName;
    }

    @Override
    public Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }

    @Override
    public List<Embedding> embedAll(List<TextSegment> segments) {
        return embeddingModel.embedAll(segments).content();
    }

    @Override
    public int dimension() {
        return embeddingModel.dimension();
    }

    @Override
    public String providerName() {
        return providerName;
    }
}
