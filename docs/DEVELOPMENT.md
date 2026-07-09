# Development

## Build & run

```bash
./gradlew build                 # compile + tests
./gradlew bootRun               # run (profiles from SPRING_PROFILES_ACTIVE)
./gradlew bootJar               # produce build/libs/rag-assistant.jar
java -jar build/libs/rag-assistant.jar
```

## Tests

`test` uses JUnit 5 and Testcontainers (PostgreSQL/pgvector). The in-process `local`
embedding provider keeps embedding tests free of external services.

```bash
./gradlew test
```

Suggested coverage:
- `DocumentIngestionService` — parse + chunk + store (Testcontainers pgvector, `local` embeddings).
- `DocumentRetrievalService` — tenant filter returns only own chunks.
- `ChatService` — answer is grounded in provided context (mock `ChatModel`).
- Adapter controllers — `@WebMvcTest` for REST DTOs and status codes.

## Code conventions

- Java 21, Spring Boot 3.4, Gradle Groovy DSL.
- Layered packages: `config / domain / embedding / ingestion / retrieval / chat / adapter / common`.
- Interfaces (`EmbeddingProvider`) hide LangChain4j specifics so providers are swappable.
- No business logic in adapters — they only map DTOs and resolve the tenant.
- Errors → `RagException` (HTTP 400) via `GlobalExceptionHandler` (`ProblemDetail`).
- Logs are structured; `TenantFilter` puts `tenant` into MDC.

## Observability

- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.
- Micrometer tracing (Brave) with Zipkin reporter (set `management.tracing.*` / Zipkin URL
  to export spans). Spans cover ingestion and retrieval steps.
- Add custom metrics in services via `MeterRegistry` (e.g. ingest duration, retrieval hits).

## Switching embeddings/chat at runtime

These are resolved at startup from `app.embedding.provider` / `app.chat.provider`.
To change without restart, externalize the decision to a `@RefreshScope` config or restart
the application (recommended for dimension changes — recreate the vector table).

## Production hardening checklist

- [ ] Move secrets to a vault / k8s secrets (never commit `.env`).
- [ ] Enable `app.security.enabled=true` and replace Basic auth with OAuth2 Resource Server.
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns the schema — already the case).
- [ ] Add pgvector `useIndex(true)` + `indexListSize` for large corpora.
- [ ] Back up the `rag` database (vectors + metadata) and the `app.storage.path` uploads.
- [ ] Add rate limiting / request size limits on `/api/documents` (already 50MB cap).
- [ ] Ship traces to a collector; alert on ingestion failures and chat latency.
