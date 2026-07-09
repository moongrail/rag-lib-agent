# ADR 0002 — Pluggable embedding provider

**Status:** Accepted

**Context.** Embeddings can come from Ollama, HuggingFace (TEI), an OpenAI-compatible API,
or a local in-process model. We must avoid locking the codebase to one vendor.

**Decision.** Define `EmbeddingProvider` (our interface) wrapping any LangChain4j
`EmbeddingModel`. `EmbeddingConfig` selects the implementation from `app.embedding.provider`.
`PgVectorEmbeddingStore` is created with `embeddingProvider.dimension()` so the table matches.

**Consequences.**
- Switch provider with a single property; zero code change.
- Default `local` (in-process all-minilm) needs no external service — great for first run.
- Constraint: changing dimension requires recreating the `rag_embeddings` table.
