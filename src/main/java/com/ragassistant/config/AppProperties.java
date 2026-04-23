package com.ragassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Tenant tenant = new Tenant();
    private Embedding embedding = new Embedding();
    private Chat chat = new Chat();
    private Retrieval retrieval = new Retrieval();
    private Ingestion ingestion = new Ingestion();
    private Security security = new Security();
    private Telegram telegram = new Telegram();
    private Storage storage = new Storage();

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    public Retrieval getRetrieval() { return retrieval; }
    public void setRetrieval(Retrieval retrieval) { this.retrieval = retrieval; }
    public Ingestion getIngestion() { return ingestion; }
    public void setIngestion(Ingestion ingestion) { this.ingestion = ingestion; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public Telegram getTelegram() { return telegram; }
    public void setTelegram(Telegram telegram) { this.telegram = telegram; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    public static class Tenant {
        private String headerName = "X-Tenant-Id";
        private String defaultTenant = "default";
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        public String getDefaultTenant() { return defaultTenant; }
        public void setDefaultTenant(String defaultTenant) { this.defaultTenant = defaultTenant; }
    }

    public static class Embedding {
        private String provider = "ollama";
        private Ollama ollama = new Ollama();
        private HuggingFace huggingface = new HuggingFace();
        private OpenAi openai = new OpenAi();
        private LmStudio lmstudio = new LmStudio();
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public Ollama getOllama() { return ollama; }
        public void setOllama(Ollama ollama) { this.ollama = ollama; }
        public HuggingFace getHuggingface() { return huggingface; }
        public void setHuggingface(HuggingFace huggingface) { this.huggingface = huggingface; }
        public OpenAi getOpenai() { return openai; }
        public void setOpenai(OpenAi openai) { this.openai = openai; }
        public LmStudio getLmstudio() { return lmstudio; }
        public void setLmstudio(LmStudio lmstudio) { this.lmstudio = lmstudio; }

        public static class Ollama {
            private String baseUrl = "http://localhost:11434";
            private String modelName = "nomic-embed-text";
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public String getModelName() { return modelName; }
            public void setModelName(String modelName) { this.modelName = modelName; }
        }

        public static class HuggingFace {
            private String baseUrl = "http://localhost:8082";
            private String modelName = "BAAI/bge-m3";
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public String getModelName() { return modelName; }
            public void setModelName(String modelName) { this.modelName = modelName; }
        }

        public static class OpenAi {
            private String baseUrl = "https://api.openai.com/v1";
            private String apiKey = "";
            private String modelName = "text-embedding-3-small";
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModelName() { return modelName; }
            public void setModelName(String modelName) { this.modelName = modelName; }
        }
    }

    public static class Chat {
        private String provider = "openrouter";
        private OpenRouter openrouter = new OpenRouter();
        private LmStudio lmstudio = new LmStudio();
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public OpenRouter getOpenrouter() { return openrouter; }
        public void setOpenrouter(OpenRouter openrouter) { this.openrouter = openrouter; }
        public LmStudio getLmstudio() { return lmstudio; }
        public void setLmstudio(LmStudio lmstudio) { this.lmstudio = lmstudio; }

        public static class OpenRouter {
            private String baseUrl = "https://openrouter.ai/api/v1";
            private String apiKey = "";
            private String modelName = "anthropic/claude-3.5-sonnet";
            private Double temperature = 0.2;
            private Integer maxTokens = 2048;
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModelName() { return modelName; }
            public void setModelName(String modelName) { this.modelName = modelName; }
            public Double getTemperature() { return temperature; }
            public void setTemperature(Double temperature) { this.temperature = temperature; }
            public Integer getMaxTokens() { return maxTokens; }
            public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        }
    }

    public static class Retrieval {
        private Integer maxResults = 5;
        private Double minScore = 0.0;
        private Integer rrfK = 60;
        public Integer getMaxResults() { return maxResults; }
        public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
        public Double getMinScore() { return minScore; }
        public void setMinScore(Double minScore) { this.minScore = minScore; }
        public Integer getRrfK() { return rrfK; }
        public void setRrfK(Integer rrfK) { this.rrfK = rrfK; }
    }

    public static class Ingestion {
        private Integer chunkSize = 1000;
        private Integer chunkOverlap = 200;
        public Integer getChunkSize() { return chunkSize; }
        public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
        public Integer getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    }

    public static class Security {
        private Boolean enabled = false;
        private String jwtSecret = "change-me";
        private String username = "rag";
        private String password = "${APP_SECURITY_PASSWORD:rag}";
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Telegram {
        private Boolean enabled = false;
        private String token = "";
        private String allowedChatIds = "";
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getAllowedChatIds() { return allowedChatIds; }
        public void setAllowedChatIds(String allowedChatIds) { this.allowedChatIds = allowedChatIds; }
    }

    public static class Storage {
        private String path = "./data/uploads";
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}
