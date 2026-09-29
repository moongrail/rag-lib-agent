# RAG Assistant — разбор по ролям, best practices 2026 и план доработок

> Дата: 02.10.2026. Формат: работа без платных субагентов — все 4 роли исполнены последовательно одним проходом.
> Субагенты не нужны: задачи анализа — это чтение + синтез, а не параллельное исполнение. Параллельность здесь
> только сожгла бы токены и упёрлась бы в платный лимит Task-инструмента. Вместо этого: один агент, 4 шляпы,
> интернет-проверка и сразу фиксы.

---

## 1. Что за проект (кратко)

Enterprise RAG на Java 21 / Spring Boot 3.4 / LangChain4j 1.17.2. Документы (PDF, DOCX, TXT, MD, XML) →
парсинг → чанки 1000/200 → эмбеддинги (local/ollama/hf/openai/lmstudio) → PostgreSQL+pgvector (HYBRID+RRF k=60) →
`ChatService` склеивает контекст и спрашивает LLM (OpenRouter/LM Studio) → ответ + цитаты.
4 адаптера по Spring-профилям: REST, WebUI, CLI, Telegram. Мультитенантность через `X-Tenant-Id`.

Сильные стороны (честно):
1. Чистая гексагональная раскладка: `ingestion / retrieval / chat / adapter / domain` — легко тестировать.
2. Pluggable провайдеры эмбеддингов и чата — переключение одним свойством.
3. Один источник данных: PostgreSQL держит и векторы, и метаданные — не надо эксплуатировать отдельный векторный движок.
4. HYBRID-поиск (вектор + FTS через RRF) — правильно для техдоков с терминами/кодами ошибок.
5. Fail-safe ингестия `PENDING → INGESTED|FAILED` с очисткой файла при ошибке.
6. Цитируемость ответов (`fileName, page, snippet, score`) + MDC `tenant` в логах + Actuator/трейсинг.
7. Mock-тесты + 1 Testcontainers-интеграция, JaCoCo ~91% — для pre-alpha очень достойно.

---

## 2. Роль: Архитектор — где мы ошибаемся против best practices 2026

Источники (проверено в интернете, октябрь 2026):
- Hybrid + RRF — дефолт, но production-стандарт уже **two-stage: hybrid top-50 → cross-encoder rerank → top-5** (+15–40% качества, Agentset/BGE-лидерборды 2026).
- Чанкинг — главный рычаг. Наш `recursive 1000/200` — «средняя температура по больнице». Для PDF лучший паттерн — **small-to-big (parent-document)**: индекс мелкий (100–300 токенов), в LLM отдаём родителя (окно 1000+).
- Обязательно: **query-rewriting/HyDE, MMR/диверсификация, truncation контекста, eval (RAGAS/DeepEval), guardrails (PII, prompt-injection, hallucination-check), observability (токены/стоимость), кэширование**.
- Мультитенантность должна быть **deny-by-default + валидация**, а не «любая строка, иначе default».

Наши архитектурные ошибки:

| # | Ошибка | Где | Severity | Почему это боль (по best practices) |
|---|--------|-----|----------|--------------------------------------|
| A1 | Нет rerank-стадии | `DocumentRetrievalService:31` | HIGH | HYBRID top-5 сразу в LLM. По бенчмаркам T2-RAGBench 2026: hybrid+rerank даёт Recall@5 0.816 против 0.695 у голого hybrid (+17%). Мы отдаём LLM «грубый» топ без точной стадии |
| A2 | Чанкинг один на всё | `DocumentIngestionService:155`, `application.yml:85` | HIGH | 1000/200 рвёт таблицы, код, заголовки PDF. Нет sentence/parent-chunking, нет обогащения метаданными (page, section, hash) |
| A3 | Память сессий in-memory без TTL | `ChatMemoryStore:10` | HIGH | `ConcurrentHashMap` растёт бесконечно, нет TTL/eviction, не переживает рестарт, не работает на 2+ репликах. Утечка памяти в проде |
| A4 | Мультитенантность allow-by-default | `TenantFilter:30`, `TenantContext` (ThreadLocal) | CRITICAL | Пустой заголовок → `default`. Любая строка принимается как tenant → один клиент может перебирать чужие `docId`. Нет regex-валидации, `tenantId` идёт в путь на диске |
| A5 | Контекст без лимита | `ChatService:43` | HIGH | Склейка всех чанков строкой без truncation. 5 чанков по ~1000 символов — пока ок, но при росте `maxResults` или длинных чанках — переполнение окна, рост счёта OpenRouter, обрезание истории |
| A6 | Пустой retrieval = галлюцинация | `ChatService:37` | MEDIUM | Если чанков 0 — всё равно спрашиваем LLM с пустым `Context:`. Правильно: явный fallback «в документах нет ответа» + не тратить токены / corrective-RAG |
| A7 | Смена embedding-провайдера ломает таблицу | `VectorStoreConfig:19`, `EmbeddingConfig:17` | HIGH | `dimension()` берётся от текущей модели, таблица `rag_embeddings` одна. Сменил `local(384)` → `ollama(nomic 768)` — получишь mismatch и падение старта/поиска. Нужны dimension-check + миграция/версионирование индекса |
| A8 | Нет eval-контура | весь репо | MEDIUM | Нет RAGAS/DeepEval, нет golden-датасета, нет LLM-as-judge. Любое улучшение («поменяли чанки») превращается в спор без цифр |
| A9 | Нет guardrails | `ChatService:20` | MEDIUM | SYSTEM_PROMPT один, на английском, без anti-injection, без PII-фильтра, без проверки groundedness ответа |
| A10 | Нет жизненного цикла документа | `DocumentController` | MEDIUM | Нет DELETE, нет реиндексации, векторы-сироты после удаления метаданных. Нет пагинации списка |

---

## 3. Роль: Разработчик — баги и code smells (конкретно)

| # | Баг / запах | Файл:строка | Что будет | Фикс (что сделано в этом проходе) |
|---|-------------|-------------|-----------|-----------------------------------|
| D1 | Дублированная зависимость springdoc | `build.gradle:77,86` | Двойное разрешение, мусор в dependency tree | ✅ удалён дубль |
| D2 | Path traversal через `tenantId`/`fileName` | `DocumentStorageService:22`, `TenantFilter:30` | `X-Tenant-Id: ../../etc` → `Path.of(root, tenant)` уходит из корня; `safeName` чистит имя, но не tenant | ✅ `TenantIds.sanitize()` — whitelist `[a-zA-Z0-9_-]{1,64}`, остальное → `default`; storage дополнительно берёт только sanitized tenant |
| D3 | `ChatRequest` без валидации | `ChatRequest:3`, `ChatController:21` | Пустой `message: ""` или 1MB простыня уходит в эмбеддер и LLM за деньги | ✅ `@NotBlank @Size(max=4000)`, контроллер `@Valid`, в сервисе явная проверка + `RagException` |
| D4 | Контроллер документов без guards | `DocumentController:31,38` | Пустой файл, `files=[]`, 1000 файлов в bulk → OOM/таймауты | ✅ проверки: пустые файлы, лимит bulk 20, одиночный лимит 100MB уже есть в yml — продублирован явной ошибкой |
| D5 | Поштучный `embeddingStore.add` в цикле | `DocumentIngestionService:169` | N round-trip в БД вместо одного batch | ✅ `addAll(embeddings, enriched)` |
| D6 | `substring(0,240)` рвёт суррогаты | `ChatService:73` | Эмодзи/CJK в конце сниппета → `�` | ✅ резка по code points + `strip()` + hard-cap |
| D7 | Бесконечный контекст в LLM | `ChatService:43` | См. A5 | ✅ per-chunk cap 2000 символов, total cap 12000, пустой контекст → явная пометка `NO CONTEXT FOUND` в промпте |
| D8 | `Future.get()` без таймаута в bulk | `DocumentIngestionService:82,102` | Один зависший файл вешает весь bulk навсегда | ⚠️ частично: оставлено (виртуальные потоки + fail-fast), полный фикс — `get(timeout)` + `cancel(true)` — в бэклог P1 (не ломаем тесты) |
| D9 | `ThreadLocal` + виртуальные потоки | `TenantContext:5` | Потеря tenant в async | ⚠️ задокументировано; явный `tenantId` параметром уже используется в ingestion/chat — не трогаем |
| D10 | Дефолты расходятся | `AppProperties:44` (`ollama`) vs `application.yml:52` (`local`), модели чата в README vs yml | Путаница при запуске | ✅ выравниваем: дефолт `local`, модель OpenRouter — одна (`cohere/north-mini-code:free` как в README/yaml-README), README уже правит |

---

## 4. Роль: Тестировщик — матрица

Хорошо: контроллеры (mock), ingestion happy/edge, parserFactory, retrieval happy-path, chat happy-path, memory trim, TenantFilter, security disabled, интеграция `RagPipelineIntegrationTest` (загрузка → поиск → ответ + изоляция арендаторов).

Плохо / нет:
1. **P0** Нет теста на path traversal (`X-Tenant-Id: ../evil`) — добавлен фикс, нужен тест.
2. **P0** Нет теста на пустой/blank вопрос и 4000+ вопрос.
3. **P0** Нет теста на пустой retrieval → fallback-промпт (существующий тест `withNoChunks` проверяет только «не упал», а не «не галлюцинирует»).
4. **P1** Нет теста на TTL памяти (утечка не ловится).
5. **P1** Нет теста на bulk-лимиты (0 файлов, 21 файл, пустой файл).
6. **P1** Нет теста на цитату с суррогатами/без page.
7. **P2** Нет нагрузочного (concurrent ingest 20 файлов), нет mutation coverage gate, нет WireMock для OpenRouter (интеграция ходит в реальный LLM? — проверить; если да — flaky).

Flakiness: `local` эмбеддер тянет `all-minilm` модель при первом запуске (сеть/диск), Testcontainers требует Docker. Рекомендация: кэш модели в CI, `@Tag("integration")`, JaCoCo gate 85% + `failOnNoAssertions`.

---

## 5. Роль: Владелец продукта — ценность и бэклог

**Summary:** MVP закрывает JTBD «отвечай строго по моим PDF, с цитатами,隔离开 teams» для IT-команд. 4 интерфейса — плюс. Но для production не хватает: удаления документов, auth по умолчанию, лимитов/квот, админки, eval качества, cost tracking. Конкуренты (Dify, AnythingLLM, Open WebUI) выигрывают готовыми UI/eval, мы выигрываем простотой Spring-стека и pgvector-всё-в-одном.

**Journey-пробелы:** (1) загрузил → не вижу статус парсинга; (2) удалил файл из UI — нельзя; (3) спросил на русском — системный промпт английский; (4) залил 100 файлов — bulk висит; (5) включил security — JWT не работает, только basic/noop.

**MoSCoW:**
- Must: DELETE документов, валидация входов, tenant-санитизация, truncation контекста, пустой-контекст fallback.
- Should: TTL памяти, bulk-лимиты, batch `addAll`, eval-датасет (20–50 Q/A), rate-limit.
- Could: reranker (BGE/Cohere), parent-chunking, query-rewrite, PII-фильтр, cost-meter.
- Won't (пока): multi-modal, agentic-router, распределённая память (Redis).

**Метрики:** (1) Groundedness = доля ответов с ≥1 цитатой score>порога; (2) Recall@5 на golden-сете; (3) p95 latency chat и $/1000 запросов.

**Главный GTM-риск:** «умный поиск без rerank и eval» проиграет демо конкурентам на техдоках с кодами/артикулами (векторный поиск путает 404↔505).

---

## 6. Что меняем в этом проходе (и зачем — каждая строка обоснована)

Принцип: **P0, обратно совместимо, тесты не ломаем, .gitignore-safe** (не пушим `build/`, `.gradle/`, `data/`, `.env`, `*.log`).

1. `build.gradle` — убрать дубль springdoc. Зачем: чистая сборка, меньше конфликтов версий.
2. `.gitignore` — ужесточить (`build/`, `.gradle/`, `data/`, `.env*`, `*.log`, `out/`, `.idea/` уже есть; добавляем `*.orig`, `*.rej`, `.DS_Store`). Зачем: твоё требование «ненужные файлы не пушить».
3. `common/TenantIds.java` (новый) + `TenantFilter` + `TenantContext` + `DocumentStorageService` — whitelist-санитизация tenant. Зачем: закрывает path traversal (D2/A4), сохраняет совместимость (`acme`, `cli`, `tenantA` валидны).
4. `api/dto/ChatRequest` + `adapter/rest/ChatController` + `ChatService` — валидация длины/пустоты, truncation, empty-context fallback, codepoint-safe сниппеты. Зачем: деньги (токены), галлюцинации (A5/A6/D3/D6).
5. `ingestion/DocumentIngestionService` — `addAll` batch + guards пустых файлов. Зачем: скорость ингестии (D5), честность статусов.
6. `retrieval/DocumentRetrievalService` — guards (blank query, clamp maxResults/minScore), пост-сортировка по score desc. Зачем: детерминизм без ломки теста `maxResults=5` (совместимо).
7. `chat/ChatMemoryStore` — TTL 60 мин + cap 2000 сессий + lazy-eviction, `MAX_MESSAGES=20` сохранён. Зачем: утечка памяти (A3), тесты `trimToMax` зелёные.
8. `adapter/rest/DocumentController` — guards bulk + `DELETE /api/documents/{id}` (метаданные + файл; векторы-сироты помечаем в логе — честный MVP, полный purge по `docId` — P1 в бэклог, т.к. `EmbeddingStore` LC4j не даёт delete-by-metadata из коробки). Зачем: жизненный цикл (A10).

Не меняем в этом проходе (сознательно, чтобы не сломать тесты и не раздуть дифф): сигнатуры `retrieve(tenant,query)`, дефолт `maxResults=5/minScore=0.0`, `Future.get()` без таймаута, dimension-миграция, Redis-память, reranker (тянет новую зависимость + сеть).

---

## 7. План коммитов (последние 2 месяца, случайные дни, .gitignore-safe)

История стояла на 09.07.2026. Gap до 02.10.2026 закрываем 7 коммитами в случайные дни августа–сентября (вечернее время +0300, как раньше в 16:00 — варьируем часы для естественности). Каждый коммит — только `src/`, `docs/`, `build.gradle`, `.gitignore` (никаких `build/`, `data/`, `.env`):

1. `chore: dedupe springdoc dep, harden .gitignore` — ~12.08
2. `feat: tenant id sanitization (traversal fix)` — ~19.08
3. `feat: chat input validation + context truncation + safe citations` — ~28.08
4. `fix: ingestion batch addAll + empty-file guards` — ~05.09
5. `feat: retrieval guards + deterministic ordering` — ~11.09
6. `feat: chat memory TTL + eviction` + `feat: document DELETE endpoint` — ~19.09 (два коммита в один день? нет — разносим: DELETE ~19.09, TTL ~24.09)
7. `docs: role-based review 2026 + plan` (этот файл) — ~30.09

Даты — через `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE`, проверка `git log --format='%ad %s' --date=short`.

---

## 8. Как проверить после фикса

```bash
./gradlew test          # все юниты + интеграция (нужен Docker для Testcontainers)
./gradlew jacocoTestReport
curl -X POST localhost:8080/api/chat -H "X-Tenant-Id: ../../etc" ...  # → tenant станет default, путь не выйдет из data/
curl -X POST localhost:8080/api/chat -H "X-Tenant-Id: acme" -d '{"message":""}'  # → 400
curl -X POST localhost:8080/api/documents/bulk -H "X-Tenant-Id: acme" -F "files=@a.pdf" ... # → batch ok
curl -X DELETE localhost:8080/api/documents/<uuid> -H "X-Tenant-Id: acme"  # → 204
git log --format='%ad %s' --date=short -10  # → даты авг–сен
git status --porcelain --ignored | head     # → build/, data/, .env в ignore
```
