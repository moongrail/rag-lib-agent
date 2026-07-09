# ADR 0004 — Hybrid retrieval (vector + keyword, RRF)

**Status:** Accepted

**Context.** Pure semantic search misses exact technical terms, error codes, and product
names common in documentation. Lexical search alone misses paraphrases.

**Decision.** Use pgvector `SearchMode.HYBRID`: cosine similarity fused with PostgreSQL
`tsvector` FTS via Reciprocal Rank Fusion (`rrf-k`, default 60).

**Consequences.**
- Better recall/precision on term-heavy tech docs than vector-only.
- Hybrid requests must supply both `queryEmbedding` and the raw `query` text (enforced in
  `DocumentRetrievalService`).
- RRF scores are rank-based (max ≈ 2/(k+1)); `min-score` is kept low.
