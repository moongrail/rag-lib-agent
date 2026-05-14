package com.ragassistant.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public interface EmbeddingProvider {

    Embedding embed(String text);

    List<Embedding> embedAll(List<TextSegment> segments);

    int dimension();

    String providerName();
}
