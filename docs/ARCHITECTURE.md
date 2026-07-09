# Architecture

## 1. Context

IT teams need a reliable assistant that answers questions **strictly from their own
technical documentation** (PDFs, specs, runbooks). Generic LLMs hallucinate on private
knowledge; a RAG system grounds answers in retrieved passages and returns citations.

This service is built as a standard Spring Boot backend with a clean hexagonal layout so
that the retrieval/LLM core is independent of the delivery channel (REST, Web UI, Telegram,
CLI).

## 2. Key design principles (enterprise)

- **Pluggable embedding strategy** — `EmbeddingProvider` wraps any LangChain4j
  `EmbeddingModel` (Ollama, HuggingFace TEI, OpenAI-compatible, LM Studio, in-process).
  Switch with one property, no code change.
- **Pluggable chat provider** — OpenRouter (OpenAI-compatible endpoint) or LM Studio.
- **Single source of truth for data** — PostgreSQL holds both vectors (pgvector) and
  document metadata (JPA). No separate vector DB to operate.
- **HYBRID retrieval** — semantic similarity (cosine) fused with keyword search
  (PostgreSQL FTS) via Reciprocal Rank Fusion (RRF). Better for term-heavy tech docs.
- **Multi-tenancy / isolation** — every chunk and document carries `tenantId`; retrieval
  filters by it, so different teams/workspaces never see each other's data.
- **Citations & auditability** — every answer returns source file names (and page when
  available) and the retrieved snippet + score.
- **Observability** — Spring Boot Actuator + Micrometer tracing across the RAG pipeline;
  structured logs with a `tenant` MDC field.
- **Fail-safe ingestion** — documents move through `PENDING → INGESTED | FAILED`; failures
  are recorded and the stored file is cleaned up.

## 3. Containers / modules

```
┌──────────────────────── Adapters (Spring profiles) ────────────────────────┐
│  rest (/api/...)   webui (Thymeleaf)   telegram (long polling)   cli        │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                 │ DTOs, validation, tenant header
┌───────────────────────────────┴─────────────────────────────────────────────┐
│  Application services                                                        │
│   DocumentIngestionService      ChatService (RAG orchestration)             │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                 │
┌───────────────────────────────┴─────────────────────────────────────────────┐
│  Domain + Infrastructure                                                    │
│   EmbeddingProvider ─► EmbeddingModel (LC4j)                                │
│   DocumentRetrievalService ─► PgVectorEmbeddingStore (HYBRID)               │
│   DocumentMetadata (JPA) ─► PostgreSQL                                       │
│   ChatModel (OpenRouter / LM Studio)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4. Request flow — chat

```
User ─► Adapter ─► TenantFilter (resolves X-Tenant-Id, sets MDC)
     └─► ChatService.ask(tenant, session, question)
           1. DocumentRetrievalService.retrieve:
                query ─embed─► vector
                PgVectorEmbeddingStore.search(HYBRID, filter=tenantId)
                ─► List<RetrievedChunk>
           2. Build messages: system + session history + user(context+question)
           3. ChatModel.chat(messages) ─► answer
           4. Persist (question, answer) to session memory
           5. Return Answer(answer, citations[fileName, page, snippet, score])
```

## 5. Request flow — ingestion

```
Upload (MultipartFile / file path)
  └─► DocumentIngestionService.ingest
       1. store bytes on disk (app.storage.path/<tenant>/...)
       2. parse: ApachePdfBoxDocumentParser | ApacheTikaDocumentParser
       3. split: DocumentSplitters.recursive(chunkSize, overlap)
       4. enrich each segment metadata: tenantId, fileName, docId
       5. embedAll(segments) ─► EmbeddingProvider
       6. embeddingStore.add(embedding, segment)  (pgvector)
       7. persist DocumentMetadata(status=INGESTED, chunkCount)
```

## 6. Data model

### pgvector — `rag_embeddings` (managed by LangChain4j)
`id`, `embedding` (vector), `text` (segment), `metadata` (JSONB: tenantId, fileName, docId).

### PostgreSQL — `documents` (managed by Flyway `V1__init.sql`)
`id uuid pk`, `tenant_id`, `title`, `file_name`, `content_type`, `storage_path`,
`status` (`PENDING|INGESTED|FAILED`), `chunk_count`, `error_message`, `created_by`,
`created_at`, `updated_at`.

## 7. ADRs

See [docs/ADR/](docs/ADR/):
- [0001 — PostgreSQL + pgvector](docs/ADR/0001-postgres-pgvector.md)
- [0002 — Pluggable embedding provider](docs/ADR/0002-pluggable-embeddings.md)
- [0003 — OpenRouter via OpenAI-compatible API](docs/ADR/0003-openrouter-openai-compatible.md)
- [0004 — Hybrid retrieval (RRF)](docs/ADR/0004-hybrid-retrieval.md)
- [0005 — LM Studio as a local provider](docs/ADR/0005-lm-studio.md)
