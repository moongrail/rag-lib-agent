package com.ragassistant.config;

import org.junit.jupiter.api.Test;

import com.ragassistant.config.LmStudio;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    @Test
    void tenant_accessors() {
        AppProperties p = new AppProperties();
        p.getTenant().setHeaderName("X-Org");
        p.getTenant().setDefaultTenant("org1");
        assertThat(p.getTenant().getHeaderName()).isEqualTo("X-Org");
        assertThat(p.getTenant().getDefaultTenant()).isEqualTo("org1");
        AppProperties.Tenant t = new AppProperties.Tenant();
        p.setTenant(t);
        assertThat(p.getTenant()).isSameAs(t);
    }

    @Test
    void embedding_accessorsAndNested() {
        AppProperties p = new AppProperties();
        p.getEmbedding().setProvider("openai");
        assertThat(p.getEmbedding().getProvider()).isEqualTo("openai");

        AppProperties.Embedding.Ollama o = new AppProperties.Embedding.Ollama();
        o.setBaseUrl("http://o"); o.setModelName("m");
        p.getEmbedding().setOllama(o);
        assertThat(p.getEmbedding().getOllama().getBaseUrl()).isEqualTo("http://o");

        AppProperties.Embedding.HuggingFace hf = new AppProperties.Embedding.HuggingFace();
        hf.setBaseUrl("http://h"); hf.setModelName("b");
        p.getEmbedding().setHuggingface(hf);
        assertThat(p.getEmbedding().getHuggingface().getModelName()).isEqualTo("b");

        AppProperties.Embedding.OpenAi oa = new AppProperties.Embedding.OpenAi();
        oa.setBaseUrl("http://oa"); oa.setApiKey("k"); oa.setModelName("t");
        p.getEmbedding().setOpenai(oa);
        assertThat(p.getEmbedding().getOpenai().getApiKey()).isEqualTo("k");

        LmStudio lm = new LmStudio();
        lm.setBaseUrl("http://lm"); lm.setApiKey("lk"); lm.setModelName("lm");
        p.getEmbedding().setLmstudio(lm);
        assertThat(p.getEmbedding().getLmstudio().getModelName()).isEqualTo("lm");

        AppProperties.Embedding e = new AppProperties.Embedding();
        p.setEmbedding(e);
        assertThat(p.getEmbedding()).isSameAs(e);
    }

    @Test
    void chat_accessorsAndNested() {
        AppProperties p = new AppProperties();
        p.getChat().setProvider("lmstudio");
        assertThat(p.getChat().getProvider()).isEqualTo("lmstudio");

        AppProperties.Chat.OpenRouter or = new AppProperties.Chat.OpenRouter();
        or.setBaseUrl("http://or"); or.setApiKey("ok"); or.setModelName("om");
        or.setTemperature(0.5); or.setMaxTokens(100);
        p.getChat().setOpenrouter(or);
        assertThat(p.getChat().getOpenrouter().getModelName()).isEqualTo("om");
        assertThat(p.getChat().getOpenrouter().getTemperature()).isEqualTo(0.5);
        assertThat(p.getChat().getOpenrouter().getMaxTokens()).isEqualTo(100);

        LmStudio lm = new LmStudio();
        lm.setBaseUrl("http://l"); lm.setApiKey("lk"); lm.setModelName("m");
        lm.setTemperature(0.3); lm.setMaxTokens(50);
        p.getChat().setLmstudio(lm);
        assertThat(p.getChat().getLmstudio().getTemperature()).isEqualTo(0.3);

        AppProperties.Chat c = new AppProperties.Chat();
        p.setChat(c);
        assertThat(p.getChat()).isSameAs(c);
    }

    @Test
    void retrieval_ingestion_security_telegram_storage_accessors() {
        AppProperties p = new AppProperties();
        p.getRetrieval().setMaxResults(7);
        p.getRetrieval().setMinScore(0.1);
        p.getRetrieval().setRrfK(42);
        assertThat(p.getRetrieval().getMaxResults()).isEqualTo(7);
        assertThat(p.getRetrieval().getMinScore()).isEqualTo(0.1);
        assertThat(p.getRetrieval().getRrfK()).isEqualTo(42);

        p.getIngestion().setChunkSize(111);
        p.getIngestion().setChunkOverlap(22);
        assertThat(p.getIngestion().getChunkSize()).isEqualTo(111);
        assertThat(p.getIngestion().getChunkOverlap()).isEqualTo(22);

        p.getSecurity().setEnabled(true);
        p.getSecurity().setJwtSecret("s");
        p.getSecurity().setUsername("u");
        p.getSecurity().setPassword("p");
        assertThat(p.getSecurity().getEnabled()).isTrue();
        assertThat(p.getSecurity().getJwtSecret()).isEqualTo("s");
        assertThat(p.getSecurity().getUsername()).isEqualTo("u");
        assertThat(p.getSecurity().getPassword()).isEqualTo("p");

        p.getTelegram().setEnabled(true);
        p.getTelegram().setToken("tok");
        p.getTelegram().setAllowedChatIds("1,2");
        assertThat(p.getTelegram().getEnabled()).isTrue();
        assertThat(p.getTelegram().getToken()).isEqualTo("tok");
        assertThat(p.getTelegram().getAllowedChatIds()).isEqualTo("1,2");

        p.getStorage().setPath("./x");
        assertThat(p.getStorage().getPath()).isEqualTo("./x");

        AppProperties.Retrieval r = new AppProperties.Retrieval();
        p.setRetrieval(r);
        AppProperties.Ingestion i = new AppProperties.Ingestion();
        p.setIngestion(i);
        AppProperties.Security sec = new AppProperties.Security();
        p.setSecurity(sec);
        AppProperties.Telegram tg = new AppProperties.Telegram();
        p.setTelegram(tg);
        AppProperties.Storage st = new AppProperties.Storage();
        p.setStorage(st);
        assertThat(p.getRetrieval()).isSameAs(r);
        assertThat(p.getIngestion()).isSameAs(i);
        assertThat(p.getSecurity()).isSameAs(sec);
        assertThat(p.getTelegram()).isSameAs(tg);
        assertThat(p.getStorage()).isSameAs(st);
    }
}
