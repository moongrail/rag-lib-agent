# RAG Research Assistant Bot

> ⚠️ **Пре-альфа (pre-alpha):** проект в активной разработке. API, структура конфигурации и схема БД могут меняться между коммитами. Не используйте в production без дополнительного ревью и тестов.

Enterprise-Ready RAG-ассистент (Retrieval-Augmented Generation) на Java 21 / Spring Boot / LangChain4j.
Загружает ваши документы (**PDF, DOCX, TXT, Markdown, XML**, в том числе пакетная загрузка),
индексирует их в PostgreSQL + pgvector и отвечает на вопросы по проиндексированной базе знаний
с использованием LLM (по умолчанию OpenRouter, локально — LM Studio). Ответы снабжаются цитатами
(имя файла, страница, фрагмент) и поддерживают мультитенантность.

---

## Содержание
- [Возможности](#возможности)
- [Стек](#стек)
- [Требования](#требования)
- [Пошаговый запуск](#пошаговый-запуск)
- [Как пользоваться](#как-пользоваться)
  - [Web UI](#web-ui)
  - [REST API](#rest-api)
  - [CLI](#cli)
  - [Telegram](#telegram)
- [Переключение провайдеров](#переключение-провайдеров)
- [Переменные окружения](#переменные-окружения)
- [Структура проекта](#структура-проекта)
- [Тесты и покрытие](#тесты-и-покрытие)
- [Документация](#документация)

---

## Возможности
- **Пакетная загрузка документов** (POST `/api/documents/bulk`, multi-file в Web UI, variadic `upload` в CLI).
- **Поддерживаемые форматы:** `pdf`, `docx`, `txt`, `md`, `markdown`, `xml` (белый список расширений).
- **Гибридный поиск** в pgvector: векторная близость + полнотекстовый поиск, объединение через **RRF** (`k=60`).
- **Переключаемые провайдеры эмбеддингов:** `ollama` | `huggingface` | `openai` | `lmstudio` | `local`
  (локальный in-process `all-minilm` по умолчанию, без внешних зависимостей).
- **Переключаемые чат-провайдеры:** `openrouter` (OpenAI-совместимый) | `lmstudio` (локально).
- **Мультитенантность** через заголовок `X-Tenant-Id` (фильтрация результатов на уровне метаданных + MDC для логов).
- **Цитаты** в ответах: имя файла, страница, фрагмент текста, score релевантности.
- **Несколько интерфейсов** через Spring-профили: `rest`, `webui`, `cli`, `telegram`.
- **Swagger/OpenAPI** UI на `/swagger-ui.html`.

## Стек
| Что | Выбор |
|-----|-------|
| Язык | Java 21 (LTS) |
| Фреймворк | Spring Boot 3.4 |
| LLM-оркестрация | LangChain4j 1.17.2 |
| LLM (чат) | OpenRouter (OpenAI-совместимый) или LM Studio |
| Эмбеддинги | Ollama / HuggingFace (TEI) / OpenAI / LM Studio / in-process (local) |
| Векторное хранилище | PostgreSQL + pgvector (HYBRID: вектор + FTS через RRF) |
| Метаданные | PostgreSQL (JPA / Flyway) |
| Интерфейсы | REST API, Web UI (Thymeleaf), Telegram, CLI (профили) |
| Сборка | Gradle (Groovy DSL) |

## Требования
- **Java 21** (JDK).
- **Docker** (для PostgreSQL + pgvector, опционально Ollama).
- Доступ к LLM:
  - OpenRouter — API-ключ с https://openrouter.ai/keys (для чата по умолчанию), либо
  - LM Studio — локально на `http://localhost:1234/v1`.
- Для эмбеддингов по умолчанию (`local`) ничего дополнительно не нужно (модель качается при первом запуске).
  Для `ollama` — запущенный Ollama с моделью `nomic-embed-text` (поднимается через docker-compose).

---

## Пошаговый запуск

### 1. Подготовка окружения
Скопируйте шаблон переменных окружения и заполните ключ OpenRouter (остальное можно оставить по умолчанию):
```bash
cp .env.example .env
# отредактируйте .env: задайте OPENROUTER_API_KEY=sk-or-...
```

### 2. Стартуйте инфраструктуру (PostgreSQL + pgvector, Ollama, Adminer)
```bash
docker compose up -d
```
- PostgreSQL+pgvector доступен на `localhost:5432` (user/password/db = `rag`/`rag`/`rag`).
- Ollama на `localhost:11434` (если выбран провайдер `ollama`).
- Adminer (GUI для БД) на `http://localhost:8081`.

### 3. Запустите приложение
Профили по умолчанию — `rest,webui`:
```bash
./gradlew bootRun
```
После старта откройте **http://localhost:8080** — загрузите PDF/MD/TXT/XML и задавайте вопросы в чате.

### 4. (альтернатива) Сборка исполняемого jar
```bash
./gradlew bootJar
java -jar build/libs/rag-assistant.jar
```

### Профили
Активные профили задаются через `SPRING_PROFILES_ACTIVE` (в `.env`) или аргументом:
```bash
SPRING_PROFILES_ACTIVE=rest,webui ./gradlew bootRun
# или
java -jar build/libs/rag-assistant.jar --spring.profiles.active=cli
```
- `rest` — REST API (`/api/documents`, `/api/chat`).
- `webui` — веб-интерфейс (Thymeleaf) на `/`.
- `cli` — интерактивная консоль.
- `telegram` — Telegram-бот (нужны `TELEGRAM_BOT_TOKEN` и `TELEGRAM_ALLOWED_CHAT_IDS`).

### Быстрая проверка здоровья
```bash
curl http://localhost:8080/actuator/health
```

---

## Как пользоваться

### Web UI
1. Откройте `http://localhost:8080`.
2. Загрузите один или несколько файлов (PDF/DOCX/TXT/MD/XML) через форму.
3. После индексации задавайте вопросы в поле чата — ответ придёт с цитатами.

### REST API
Все эндпоинты требуют заголовок арендатора `X-Tenant-Id` (любая строка, например `acme`).

**Загрузка одного документа:**
```bash
curl -X POST http://localhost:8080/api/documents \
  -H "X-Tenant-Id: acme" \
  -F "file=@./docs/README.md"
```

**Пакетная загрузка (несколько файлов сразу):**
```bash
curl -X POST http://localhost:8080/api/documents/bulk \
  -H "X-Tenant-Id: acme" \
  -F "files=@./a.pdf" -F "files=@./b.md" -F "files=@./c.txt"
```

**Список проиндексированных документов:**
```bash
curl http://localhost:8080/api/documents -H "X-Tenant-Id: acme"
```

**Получить документ по id:**
```bash
curl http://localhost:8080/api/documents/<UUID> -H "X-Tenant-Id: acme"
```

**Задать вопрос (чат):**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "X-Tenant-Id: acme" \
  -H "Content-Type: application/json" \
  -d '{"message":"Какова политика возврата?","sessionId":"sess-1"}'
```
Ответ содержит `answer`, `sessionId` и массив `citations`
(`fileName`, `page`, `snippet`, `score`). `sessionId` можно не передавать — тогда создаётся новая сессия.

Swagger UI: http://localhost:8080/swagger-ui.html (спецификация — `/v3/api-docs`).

### CLI
Запуск в профиле `cli`:
```bash
java -jar build/libs/rag-assistant.jar --spring.profiles.active=cli
```
Команды (арендатор фиксированный — `cli`):
```
upload <путь> [<путь> ...]   # загрузить один или несколько файлов
ask <вопрос>                  # задать вопрос по проиндексированным документам
exit                          # выход
```
Пример:
```
> upload ./doc1.pdf ./notes.md
Ingested 2/2 file(s).
> ask Что написано в doc1.pdf?
Ответ ассистента...
```

### Telegram
1. Создайте бота через @BotFather, получите токен.
2. В `.env` задайте:
   ```
   TELEGRAM_BOT_TOKEN=123456:ABC-DEF...
   TELEGRAM_ALLOWED_CHAT_IDS=1111111,2222222
   SPRING_PROFILES_ACTIVE=telegram
   ```
3. Запустите приложение. В боте отправьте `/start`, затем задавайте вопросы — бот отвечает по вашим документам
   (каждый чат = отдельный арендатор по `chatId`).

---

## Переключение провайдеров

### Chat-провайдер (`app.chat.provider`)
- `openrouter` (по умолчанию) — нужен `OPENROUTER_API_KEY`. Модель по умолчанию `cohere/north-mini-code:free`
  (меняется в `application.yml` → `app.chat.openrouter.model-name`).
- `lmstudio` — локально, `http://localhost:1234/v1`, модель `omnicoder-qwen3.5-9b-...`.

### Embedding-провайдер (`app.embedding.provider`)
- `local` (по умолчанию) — in-process `all-minilm`, без внешних сервисов.
- `ollama` — `http://localhost:11434`, модель `nomic-embed-text`.
- `huggingface` — TEI-совместимый эндпоинт + `modelId`.
- `openai` / `lmstudio` — OpenAI-совместимый API, нужен `EMBEDDING_API_KEY`.

Пример (`.env`):
```
EMBEDDING_PROVIDER=local
CHAT_PROVIDER=openrouter
OPENROUTER_API_KEY=sk-or-...
```

---

## Переменные окружения
| Переменная | Назначение | По умолчанию |
|-----------|-----------|--------------|
| `SPRING_PROFILES_ACTIVE` | Активные профили (`rest,webui` и т.п.) | `rest,webui` |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | Доступы БД (docker-compose) | `rag` / `rag` / `rag` |
| `SPRING_DATASOURCE_URL` | JDBC-URL к PostgreSQL+pgvector | `jdbc:postgresql://localhost:5432/rag` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Пользователь/пароль БД | `rag` / `rag` |
| `OPENROUTER_API_KEY` | Ключ OpenRouter (чат) | — |
| `EMBEDDING_PROVIDER` | Провайдер эмбеддингов | `local` |
| `EMBEDDING_API_KEY` | Ключ для `openai`-провайдера эмбеддингов | — |
| `CHAT_PROVIDER` | Провайдер чата | `openrouter` |
| `TELEGRAM_BOT_TOKEN` | Токен Telegram-бота | — |
| `TELEGRAM_ALLOWED_CHAT_IDS` | Разрешённые chat-id (через запятую) | — |
| `JWT_SECRET` | Секрет JWT (если включена security) | `change-me` |

> Никогда не коммитьте реальные ключи. Используйте `.env` (он в `.gitignore`) и `.env.example` как шаблон.

---

## Структура проекта
```
src/main/java/com/ragassistant/
  config/        Spring/LangChain4j бины, AppProperties
  domain/        DocumentMetadata (JPA), репозиторий, enum статуса
  embedding/     EmbeddingProvider (стратегия) + адаптер LangChain
  ingestion/     парсинг PDF/DOCX/TXT, чанкование, эмбеддинг, сохранение
  retrieval/     HYBRID-поиск в pgvector с фильтром по арендатору
  chat/          оркестрация RAG, память сессий, цитаты
  adapter/       rest | webui | cli | telegram (по профилям)
  common/        контекст арендатора, servlet-фильтр, исключения
  api/dto/       записи запроса/ответа
src/main/resources/
  application.yml + application-{rest,webui,telegram,cli}.yml
  db/migration/V1__init.sql (расширение vector + таблица documents)
  templates/chat.html (Web UI)
docs/            architecture, setup, configuration, rag-pipeline, api, adr/ (EN + RU)
```

---

## Тесты и покрытие
- Сборка и тесты: `./gradlew build` (включает JaCoCo-отчёт в `build/reports/jacoco/test`).
- Покрытие инструкций — **~91%** (mock-based юнит-тесты + 1 интеграционный тест на реальном PostgreSQL+pgvector).
- Интеграционный тест поднимает реальный pgvector и проверяет полный контур:
  загрузка → гибридный поиск → ответ с цитатами, а также изоляцию арендаторов.

---

## Документация
- [Архитектура](docs/ARCHITECTURE.md)
- [Установка и локальный запуск](docs/SETUP.md)
- [Конфигурация и переключение провайдеров](docs/CONFIGURATION.md)
- [RAG-конвейер](docs/RAG_PIPELINE.md)
- [API-справочник](docs/API.md)
- [Руководство пользователя (пошагово)](docs/USAGE.md)
- [Краткое описание проекта (для резюме)](docs/PROJECT_SUMMARY.md)
- [Разработка](docs/DEVELOPMENT.md)
- [ADR](docs/ADR/)

Русские версии — в [docs/ru/](docs/ru/) (включая [USAGE](docs/ru/USAGE.md) и [summary](docs/ru/PROJECT_SUMMARY.md)).

---

## Лицензия
[MIT](LICENSE) © 2025 moongrail.
