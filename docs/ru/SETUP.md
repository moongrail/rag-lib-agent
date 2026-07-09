# Установка и локальный запуск

## 1. Требования

- Java 21
- Docker (для PostgreSQL/pgvector, опционально Ollama)
- Ключ OpenRouter (https://openrouter.ai/keys) — либо локально запущенный LM Studio

## 2. Инфраструктура

```bash
docker compose up -d
```

Поднимает:

| Сервис  | Образ | Порт | Назначение |
|---------|-------|------|------------|
| postgres | `pgvector/pgvector:pg17` | 5432 | векторы + метаданные |
| ollama   | `ollama/ollama:latest`   | 11434 | локальные эмбеддинги (опц.) |
| adminer  | `adminer:4`              | 8081 | веб-интерфейс к БД |

Учётные данные БД по умолчанию: `rag` / `rag`, база `rag` (переопределяются через `.env`).

> Расширение `vector` и таблица `documents` создаются автоматически при старте
> (Flyway `V1__init.sql`). Таблица `rag_embeddings` создаётся LangChain4j pgvector при
> первой записи.

## 3. Конфигурация приложения

Скопируйте шаблон окружения и заполните минимум ключ чата:

```bash
cp .env.example .env
# отредактируйте .env: OPENROUTER_API_KEY=sk-or-...
```

Активируйте интерфейсы через `SPRING_PROFILES_ACTIVE` (через запятую): `rest`, `webui`,
`telegram`, `cli`.

```bash
# REST + Web UI (по умолчанию)
./gradlew bootRun

# Только CLI (интерактивная консоль)
SPRING_PROFILES_ACTIVE=cli ./gradlew bootRun

# REST + Telegram
SPRING_PROFILES_ACTIVE=rest,telegram TELEGRAM_BOT_TOKEN=... ./gradlew bootRun
```

## 4. Варианты провайдера эмбеддингов

| `app.embedding.provider` | Нужно | Примечание |
|--------------------------|-------|-----------|
| `local` (по умолчанию) | ничего | in-process all-minilm, 384-dim. Без внешних зависимостей — лучший первый запуск |
| `ollama` | Ollama на :11434 | `nomic-embed-text` и т.п. |
| `huggingface` | TEI-сервер | напр. `BAAI/bge-m3` |
| `openai` | API-ключ | любой OpenAI-совместимый эндпоинт эмбеддингов |
| `lmstudio` | LM Studio :1234 | OpenAI-совместимые эмбеддинги |

## 5. Варианты провайдера чата

| `app.chat.provider` | Эндпоинт | Модель по умолчанию |
|---------------------|----------|---------------------|
| `openrouter` | `https://openrouter.ai/api/v1` | `cohere/north-mini-code:free` |
| `lmstudio` | `http://127.0.0.1:1234/v1` | `omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2` |

## 6. Дымовой тест (без UI)

Поддерживаемые форматы: **PDF, DOCX, TXT, MD, XML**. Массовая загрузка поддержана.

```bash
# 1) массовая загрузка нескольких файлов (tenant = default)
curl.exe -s -F "files=@/path/to/manual.pdf" -F "files=@/path/to/spec.docx" -F "files=@/path/to/notes.xml" http://localhost:8080/api/documents/bulk

# 2) спросить
$body = @{message='What is the refund policy?'} | ConvertTo-Json
curl.exe -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d "@chat.json"
```

Для мультитенантной изоляции передавайте заголовок `X-Tenant-Id: acme`.
