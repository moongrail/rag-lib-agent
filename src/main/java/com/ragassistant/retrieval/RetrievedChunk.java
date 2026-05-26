package com.ragassistant.retrieval;

import dev.langchain4j.data.document.Metadata;

public record RetrievedChunk(String text, Metadata metadata, double score) {
}
