package com.ragassistant.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import com.ragassistant.embedding.EmbeddingProvider;
import com.ragassistant.embedding.LangChainEmbeddingProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingConfigTest {

    private final EmbeddingConfig config = new EmbeddingConfig();

    private AppProperties propsWith(String provider) {
        AppProperties p = new AppProperties();
        p.getEmbedding().setProvider(provider);
        return p;
    }

    @Test
    void local_returnsQuantizedModel() {
        EmbeddingModel model = config.embeddingModel(propsWith("local"));
        assertThat(model).isInstanceOf(AllMiniLmL6V2QuantizedEmbeddingModel.class);
    }

    @Test
    void ollama_returnsOllamaModel() {
        EmbeddingModel model = config.embeddingModel(propsWith("ollama"));
        assertThat(model).isInstanceOf(OllamaEmbeddingModel.class);
    }

    @Test
    void openai_returnsOpenAiModel() {
        EmbeddingModel model = config.embeddingModel(propsWith("openai"));
        assertThat(model).isInstanceOf(OpenAiEmbeddingModel.class);
    }

    @Test
    void huggingface_requiresAccessToken() {
        assertThatThrownBy(() -> config.embeddingModel(propsWith("huggingface")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void lmstudio_returnsOpenAiModel() {
        EmbeddingModel model = config.embeddingModel(propsWith("lmstudio"));
        assertThat(model).isInstanceOf(OpenAiEmbeddingModel.class);
    }

    @Test
    void unknownProvider_throws() {
        assertThatThrownBy(() -> config.embeddingModel(propsWith("bogus")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void embeddingProvider_wrapsModel() {
        EmbeddingModel model = config.embeddingModel(propsWith("local"));
        EmbeddingProvider provider = config.embeddingProvider(model, propsWith("local"));
        assertThat(provider).isInstanceOf(LangChainEmbeddingProvider.class);
        assertThat(provider.providerName()).isEqualTo("local");
    }
}
