# Configuration & Provider Switching

All configuration lives under the `app.*` namespace and is bound to
`com.ragassistant.config.AppProperties` via `@ConfigurationProperties`.

## Top-level

| Key | Default | Description |
|-----|---------|-------------|
| `app.tenant.header-name` | `X-Tenant-Id` | HTTP header carrying the tenant |
| `app.tenant.default` | `default` | tenant used when header is absent |
| `app.storage.path` | `./data/uploads` | where uploaded originals are stored |
| `app.security.enabled` | `false` | enable HTTP Basic auth (demo) |
| `app.security.username` | `rag` | basic-auth user |
| `app.security.password` | `rag` | basic-auth password (use `${APP_SECURITY_PASSWORD}`) |
| `app.retrieval.max-results` | `5` | chunks returned per query |
| `app.retrieval.min-score` | `0.0` | score floor (RRF is rank-based, keep low) |
| `app.retrieval.rrf-k` | `60` | RRF sensitivity (lower = top-heavy) |
| `app.ingestion.chunk-size` | `1000` | tokens per chunk |
| `app.ingestion.chunk-overlap` | `200` | overlap tokens |

## Embedding provider — `app.embedding.provider`

`ollama | huggingface | openai | lmstudio | local`

```yaml
app:
  embedding:
    provider: local            # <-- switch here
    ollama:      { base-url: http://localhost:11434, model-name: nomic-embed-text }
    huggingface: { base-url: http://localhost:8082, model-name: BAAI/bge-m3 }
    openai:      { base-url: https://api.openai.com/v1, api-key: ${EMBEDDING_API_KEY}, model-name: text-embedding-3-small }
    lmstudio:    { base-url: http://127.0.0.1:1234/v1, api-key: lm-studio, model-name: omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2 }
```

> **Dimension matters.** The pgvector table is created with the dimension of the active
> embedding model. Switching provider (and thus dimension) requires recreating the
> `rag_embeddings` table (or using a fresh database).

## Chat provider — `app.chat.provider`

`openrouter | lmstudio`

```yaml
app:
  chat:
    provider: openrouter       # <-- switch here
    openrouter:
      base-url: https://openrouter.ai/api/v1
      api-key:  ${OPENROUTER_API_KEY}
      model-name: cohere/north-mini-code:free
      temperature: 0.2
      max-tokens: 2048
    lmstudio:
      base-url: http://127.0.0.1:1234/v1
      api-key: lm-studio
      model-name: omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2
      temperature: 0.2
      max-tokens: 2048
```

## PostgreSQL / pgvector

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/rag}
    username: ${SPRING_DATASOURCE_USERNAME:rag}
    password: ${SPRING_DATASOURCE_PASSWORD:rag}
```

## Telegram

```yaml
app:
  telegram:
    enabled: true
    token: ${TELEGRAM_BOT_TOKEN}
    allowed-chat-ids: "123456789,987654321"   # empty = allow all
```
Activate with profile `telegram`. Per-chat isolation uses the Telegram `chatId` as tenant.

## Profiles

| Profile | What it enables |
|---------|-----------------|
| `rest`   | REST controllers under `/api/*` + security filter |
| `webui`  | Thymeleaf UI at `/` and `/ui/documents` upload |
| `telegram` | long-polling Telegram bot |
| `cli` | interactive console (`upload <path>`, `ask <q>`, `exit`) |

Security config only applies under `rest`/`webui`.
