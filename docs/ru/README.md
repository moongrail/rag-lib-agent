# RAG Research Assistant Bot (на русском)

Корпоративный ассистент с Retrieval-Augmented Generation. Загружает ваши PDF / DOCX / TXT /
Markdown / **XML** документы (поддержана массовая загрузка), векторизует их в PostgreSQL +
pgvector и отвечает на вопросы по проиндексированным знаниям с помощью LLM (по умолчанию
OpenRouter, локально — LM Studio).

## Стек

| Задача          | Решение |
|-----------------|---------|
| Язык            | Java 21 (LTS) |
| Фреймворк       | Spring Boot 3.4 |
| Оркестрация LLM | LangChain4j 1.17.2 |
| LLM (чат)       | OpenRouter (совместимый с OpenAI API) или LM Studio |
| Эмбеддинги      | Подключаемые: Ollama / HuggingFace (TEI) / OpenAI / LM Studio / локально (in-process) |
| Векторное хранилище | PostgreSQL + pgvector (HYBRID: вектор + FTS через RRF) |
| Метаданные      | PostgreSQL (JPA / Flyway) |
| Интерфейсы      | REST API, Web UI (Thymeleaf), Telegram, CLI (через профили) |
| Сборка          | Gradle (Groovy DSL) |

## Быстрый старт

```bash
cp .env.example .env          # заполните OPENROUTER_API_KEY и т.д.
docker compose up -d          # postgres + pgvector + ollama + adminer
./gradlew bootRun             # старт с профилями: rest,webui
```

Откройте http://localhost:8080 — загрузите PDF и задавайте вопросы в чате.

## Переключение провайдеров

Эмбеддинги (`app.embedding.provider`): `ollama | huggingface | openai | lmstudio | local`
Чат (`app.chat.provider`): `openrouter | lmstudio`

См. [docs/ru/CONFIGURATION.md](docs/ru/CONFIGURATION.md) и [docs/ru/SETUP.md](docs/ru/SETUP.md).

## Документация

- [Архитектура](docs/ru/ARCHITECTURE.md)
- [Установка и локальный запуск](docs/ru/SETUP.md)
- [Конфигурация и переключение провайдеров](docs/ru/CONFIGURATION.md)
- [RAG-конвейер](docs/ru/RAG_PIPELINE.md)
- [Справочник API](docs/ru/API.md)
- [Руководство пользователя (как запустить по шагам)](docs/ru/USAGE.md)
- [Описание проекта (для резюме)](docs/ru/PROJECT_SUMMARY.md)
- [Разработка](docs/ru/DEVELOPMENT.md)
- [ADR (архитектурные решения)](docs/ru/ADR/)

Англоязычные версии: см. [docs/](docs/).
