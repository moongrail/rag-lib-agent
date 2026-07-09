# ADR 0001 — PostgreSQL + pgvector as the vector store

**Status:** Accepted

**Context.** We need vector storage for RAG. Options: dedicated vector DBs (Qdrant,
Milvus, Chroma, Elasticsearch) vs. PostgreSQL with the `pgvector` extension.

**Decision.** Use PostgreSQL + `pgvector`.

**Consequences.**
- Single datastore for both vectors and document metadata → one backup/restore, one
  connection pool, transactional consistency between `rag_embeddings` and `documents`.
- Mature ops tooling, familiar to enterprise teams.
- HYBRID search (vector + FTS) is built in via `SearchMode.HYBRID` + RRF.
- Trade-off: raw vector throughput is lower than a tuned dedicated engine; mitigated by
  IVFFlat/HNSW index (`useIndex`) for large corpora.
