# RAG Pipeline

## Ingestion (indexing)

1. **Receive** — `DocumentIngestionService.ingest(...)` gets bytes from an upload or file path.
2. **Store original** — bytes are written to `app.storage.path/<tenant>/<uuid>_<name>`
   so failed ingestions can be retried and the source is auditable.
3. **Parse**
   - PDF → `ApachePdfBoxDocumentParser`
   - everything else (DOCX, TXT, MD) → `ApacheTikaDocumentParser`
4. **Chunk** — `DocumentSplitters.recursive(chunkSize, overlap)` (token-aware recursive split).
   Defaults: 1000 tokens / 200 overlap.
5. **Enrich metadata** — each `TextSegment` gets `tenantId`, `fileName`, `docId`.
6. **Embed** — `EmbeddingProvider.embedAll(segments)` → `List<Embedding>`.
7. **Store** — `embeddingStore.add(embedding, segment)` into pgvector (`rag_embeddings`).
8. **Record** — `DocumentMetadata` saved with `status=INGESTED`, `chunkCount`. On any error
   status becomes `FAILED` with `error_message` and the file is deleted.

## Retrieval (online)

1. **Embed query** — `EmbeddingProvider.embed(question)`.
2. **Hybrid search** — `PgVectorEmbeddingStore.search(...)` with `SearchMode.HYBRID`:
   - **Vector** leg: cosine similarity over `embedding`.
   - **Keyword** leg: PostgreSQL `tsvector` FTS over the segment text.
   - **Fusion**: Reciprocal Rank Fusion `1/(k+rank_vector) + 1/(k+rank_keyword)` (k=`rrf-k`).
   - The hybrid request **requires both** `queryEmbedding` and `query` text.
3. **Tenant isolation** — `metadataKey("tenantId").isEqualTo(tenantId)` filter is applied so
   a tenant only retrieves its own chunks.
4. **Result** — `List<RetrievedChunk>` (text + metadata + score), capped at `max-results`.

## Generation

1. `ChatService` concatenates retrieved chunks into a `Context:` block.
2. Messages: `System` (grounding instructions) + session **history** + `User(context+question)`.
3. `ChatModel.chat(messages)` (OpenRouter or LM Studio) returns the answer.
4. The `(question, answer)` pair is appended to the per-session window memory (last 20 turns).
5. Citations are derived from the retrieved chunks: `fileName`, `page` (when present),
   `snippet` (truncated), `score`.

## Tuning checklist

- **Noisy retrieval** → lower `max-results` (3–5), raise `chunk-size` for broader context.
- **Missing exact terms** → ensure `SearchMode.HYBRID` (default) and `textSearchConfig`
  matches your language (`simple` / `english` / …).
- **Hallucinations** → the system prompt forbids answering outside the context; keep it.
- **Latency at scale** → enable `useIndex(true)` + `indexListSize` on the pgvector store
  (see `VectorStoreConfig`) for >100k embeddings.

## Notes / future

- **Page-level citations**: the PDF parser currently stores document-level metadata.
  Page tracking can be added by parsing per-page and tagging `page` in segment metadata
  (the retrieval/citation code already reads `page`).
- **Reranking**: a cross-encoder reranker can be inserted between retrieval and generation
  for higher precision (LangChain4j `ScoringModel`).
