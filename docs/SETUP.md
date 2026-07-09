# Setup & Local Run

## 1. Prerequisites

- Java 21
- Docker (for PostgreSQL/pgvector, optional Ollama)
- An OpenRouter API key (https://openrouter.ai/keys) — or LM Studio running locally

## 2. Infrastructure

```bash
docker compose up -d
```

Brings up:

| Service  | Image | Port | Purpose |
|----------|-------|------|---------|
| postgres | `pgvector/pgvector:pg17` | 5432 | vectors + metadata |
| ollama   | `ollama/ollama:latest`   | 11434 | local embeddings (optional) |
| adminer  | `adminer:4`              | 8081 | DB UI |

Default DB credentials: `rag` / `rag`, database `rag` (override via `.env`).

> The `vector` extension and the `documents` table are created automatically on startup
> (Flyway `V1__init.sql`). The `rag_embeddings` table is created by LangChain4j pgvector
> on first write.

## 3. Application configuration

Copy the env template and fill at least the chat key:

```bash
cp .env.example .env
# edit .env: OPENROUTER_API_KEY=sk-or-...
```

Activate interfaces via `SPRING_PROFILES_ACTIVE` (comma-separated): `rest`, `webui`,
`telegram`, `cli`.

```bash
# REST + Web UI (default)
./gradlew bootRun

# Only CLI (interactive console)
SPRING_PROFILES_ACTIVE=cli ./gradlew bootRun

# REST + Telegram
SPRING_PROFILES_ACTIVE=rest,telegram TELEGRAM_BOT_TOKEN=... ./gradlew bootRun
```

## 4. Embedding provider options

| `app.embedding.provider` | Needs | Notes |
|--------------------------|-------|-------|
| `local` (default) | nothing | in-process all-minilm, 384-dim. Zero external deps — best for first run |
| `ollama` | Ollama on :11434 | `nomic-embed-text` etc. |
| `huggingface` | TEI server | e.g. `BAAI/bge-m3` |
| `openai` | API key | any OpenAI-compatible embeddings endpoint |
| `lmstudio` | LM Studio :1234 | OpenAI-compatible embeddings |

## 5. Chat provider options

| `app.chat.provider` | Endpoint | Default model |
|---------------------|----------|---------------|
| `openrouter` | `https://openrouter.ai/api/v1` | `cohere/north-mini-code:free` |
| `lmstudio` | `http://127.0.0.1:1234/v1` | `omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2` |

## 6. Smoke test (no UI)

Supported formats: **PDF, DOCX, TXT, MD, XML**. Bulk upload is supported.

```bash
# 1) bulk upload several files (tenant resolves to default)
curl.exe -s -F "files=@/path/to/manual.pdf" -F "files=@/path/to/spec.docx" -F "files=@/path/to/notes.xml" http://localhost:8080/api/documents/bulk

# 2) ask (PowerShell: put JSON in a file to avoid quoting issues)
Set-Content -Path chat.json -Value '{"message":"What is the refund policy?"}' -Encoding utf8
curl.exe -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d "@chat.json"
```

For multi-tenant isolation pass the header `X-Tenant-Id: acme`.

For multi-tenant isolation pass the header `X-Tenant-Id: acme`.
