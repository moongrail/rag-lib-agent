package com.ragassistant.config;

import com.ragassistant.embedding.EmbeddingProvider;
import com.ragassistant.embedding.LangChainEmbeddingProvider;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(AppProperties props) {
        AppProperties.Embedding e = props.getEmbedding();
        return switch (e.getProvider().toLowerCase()) {
            case "ollama" -> OllamaEmbeddingModel.builder()
                    .baseUrl(e.getOllama().getBaseUrl())
                    .modelName(e.getOllama().getModelName())
                    .build();
            case "huggingface" -> HuggingFaceEmbeddingModel.builder()
                    .baseUrl(e.getHuggingface().getBaseUrl())
                    .modelId(e.getHuggingface().getModelName())
                    .build();
            case "openai" -> OpenAiEmbeddingModel.builder()
                    .baseUrl(e.getOpenai().getBaseUrl())
                    .apiKey(e.getOpenai().getApiKey())
                    .modelName(e.getOpenai().getModelName())
                    .build();
            case "lmstudio" -> OpenAiEmbeddingModel.builder()
                    .baseUrl(e.getLmstudio().getBaseUrl())
                    .apiKey(e.getLmstudio().getApiKey())
                    .modelName(e.getLmstudio().getModelName())
                    .build();
            case "local" -> new AllMiniLmL6V2QuantizedEmbeddingModel();
            default -> throw new IllegalArgumentException("Unknown embedding provider: " + e.getProvider());
        };
    }

    @Bean
    public EmbeddingProvider embeddingProvider(EmbeddingModel embeddingModel, AppProperties props) {
        return new LangChainEmbeddingProvider(embeddingModel, props.getEmbedding().getProvider());
    }
}
