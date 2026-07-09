# Project Summary (for resume)

**RAG Research Assistant Bot** — an enterprise RAG assistant that answers questions grounded
in your private technical documentation.

## Key features
- Bulk document ingestion in **PDF, DOCX, TXT, Markdown, XML**.
- Automated indexing: parse → recursive chunking → embed → store in pgvector.
- Hybrid retrieval (vector similarity + PostgreSQL full-text search, fused via RRF).
- LLM answers with citations to source documents and conversation memory.
- Multi-tenant isolation (filtered by `tenantId`).
- Four delivery channels: **REST API, Web UI, Telegram, CLI** (via Spring profiles).
- Pluggable embedding providers (Ollama / HuggingFace / OpenAI / LM Studio / local) and
  chat providers (OpenRouter / LM Studio) — switch with a single property.
- Observability (Actuator + Micrometer) and centralized error handling.

## Tech stack
Java 21 · Spring Boot 3.4 · LangChain4j 1.17 · PostgreSQL + pgvector · Flyway ·
Docker · Gradle (Groovy DSL) · Spring Security · Micrometer / Actuator.
