# Архитектура

## 1. Контекст

IT-командам нужен надёжный ассистент, который отвечает на вопросы **строго по своей
внутренней технической документации** (PDF, спецификации, регламенты). Общие LLM
галлюцинируют по закрытым знаниям; RAG-система опирает ответ на найденные фрагменты и
возвращает цитаты.

Сервис построен как стандартное Spring Boot-приложение с чётким гексагональным делением,
чтобы ядро (ретривл + LLM) было независимо от канала доставки (REST, Web UI, Telegram, CLI).

## 2. Ключевые принципы (enterprise)

- **Подключаемые эмбеддинги** — `EmbeddingProvider` оборачивает любую LangChain4j-модель
  (`EmbeddingModel`): Ollama, HuggingFace TEI, OpenAI-совместимую, LM Studio, in-process.
  Переключается одной настройкой.
- **Подключаемый провайдер чата** — OpenRouter (OpenAI-совместимый endpoint) или LM Studio.
- **Единый источник данных** — PostgreSQL хранит и векторы (pgvector), и метаданные
  документов (JPA). Не нужен отдельный vector-DB.
- **HYBRID-поиск** — семантика (косинус) + ключевой поиск (PostgreSQL FTS) через
  Reciprocal Rank Fusion (RRF). Лучше для терминологичной техдокументации.
- **Мультитенантность / изоляция** — каждый чанк и документ несёт `tenantId`; поиск
  фильтруется по нему, команды не видят данные друг друга.
- **Цитаты и аудит** — каждый ответ возвращает имя исходного файла (и страницу, если есть),
  найденный фрагмент и оценку релевантности.
- **Наблюдаемость** — Spring Boot Actuator + Micrometer-трейсинг по шагам RAG-конвейера;
  структурированные логи с полем MDC `tenant`.
- **Устойчивая загрузка** — документы проходят `PENDING → INGESTED | FAILED`; сбои
  сохраняются, загруженный файл удаляется.

## 3. Модули / слои

```
┌─────────────── Адаптеры (профили Spring) ──────────────────┐
│  rest (/api/...)   webui (Thymeleaf)   telegram   cli      │
└────────────────────────┬───────────────────────────────────┘
                         │ DTO, валидация, заголовок tenant
┌────────────────────────┴───────────────────────────────────┐
│  Сервисы приложения                                        │
│   DocumentIngestionService      ChatService (оркестрация)  │
└────────────────────────┬───────────────────────────────────┘
                         │
┌────────────────────────┴───────────────────────────────────┐
│  Домен + инфраструктура                                    │
│   EmbeddingProvider ─► EmbeddingModel (LC4j)                │
│   DocumentRetrievalService ─► PgVectorEmbeddingStore (HYBRID)│
│   DocumentMetadata (JPA) ─► PostgreSQL                     │
│   ChatModel (OpenRouter / LM Studio)                        │
└────────────────────────────────────────────────────────────┘
```

## 4. Поток запроса — чат

```
User ─► Adapter ─► TenantFilter (берёт X-Tenant-Id, ставит MDC)
     └─► ChatService.ask(tenant, session, question)
           1. DocumentRetrievalService.retrieve:
                query ─embed─► вектор
                PgVectorEmbeddingStore.search(HYBRID, filter=tenantId)
                ─► List<RetrievedChunk>
           2. Сборка сообщений: system + история сессии + user(контекст+вопрос)
           3. ChatModel.chat(messages) ─► ответ
           4. Сохранение (вопрос, ответ) в память сессии
           5. Возврат Answer(ответ, цитаты[файл, страница, фрагмент, оценка])
```

## 5. Поток запроса — загрузка

```
Upload (MultipartFile / путь к файлу)
  └─► DocumentIngestionService.ingest
       1. сохранить байты на диск (app.storage.path/<tenant>/...)
       2. парсинг: ApachePdfBoxDocumentParser | ApacheTikaDocumentParser
       3. разбиение: DocumentSplitters.recursive(chunkSize, overlap)
       4. обогащение метаданными: tenantId, fileName, docId
       5. embedAll(segments) ─► EmbeddingProvider
       6. embeddingStore.add(embedding, segment)  (pgvector)
       7. сохранение DocumentMetadata(status=INGESTED, chunkCount)
```

## 6. Модель данных

### pgvector — `rag_embeddings` (создаётся LangChain4j)
`id`, `embedding` (vector), `text` (сегмент), `metadata` (JSONB: tenantId, fileName, docId).

### PostgreSQL — `documents` (создаётся Flyway `V1__init.sql`)
`id uuid pk`, `tenant_id`, `title`, `file_name`, `content_type`, `storage_path`,
`status` (`PENDING|INGESTED|FAILED`), `chunk_count`, `error_message`, `created_by`,
`created_at`, `updated_at`.

## 7. ADR

См. [docs/ru/ADR/](docs/ru/ADR/):
- [0001 — PostgreSQL + pgvector](docs/ru/ADR/0001-postgres-pgvector.md)
- [0002 — Подключаемые эмбеддинги](docs/ru/ADR/0002-pluggable-embeddings.md)
- [0003 — OpenRouter через OpenAI-совместимый API](docs/ru/ADR/0003-openrouter-openai-compatible.md)
- [0004 — Гибридный поиск (RRF)](docs/ru/ADR/0004-hybrid-retrieval.md)
- [0005 — LM Studio как локальный провайдер](docs/ru/ADR/0005-lm-studio.md)
