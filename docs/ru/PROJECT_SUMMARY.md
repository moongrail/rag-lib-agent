# Описание проекта (для резюме)

**RAG Research Assistant Bot** — корпоративный ассистент на базе RAG для ответов по
собственной технической документации.

## Краткие возможности
- Массовая загрузка документов (bulk) в форматах **PDF, DOCX, TXT, Markdown, XML**.
- Автоматическая индексация: парсинг → рекурсивное разбиение на чанки → эмбеддинг →
  сохранение в pgvector.
- Гибридный поиск (семантика + полнотекстовый поиск PostgreSQL, слияние через RRF).
- Ответы LLM с цитатами на исходные документы и памятью диалога.
- Мультитенантная изоляция (фильтрация по `tenantId`).
- 4 интерфейса доставки: **REST API, Web UI, Telegram, CLI** (через Spring-профили).
- Подключаемые провайдеры эмбеддингов (Ollama / HuggingFace / OpenAI / LM Studio / local)
  и чата (OpenRouter / LM Studio) — переключение одной настройкой.
- Наблюдаемость (Actuator + Micrometer) и централизованная обработка ошибок.

## Технологии
Java 21 · Spring Boot 3.4 · LangChain4j 1.17 · PostgreSQL + pgvector · Flyway ·
Docker · Gradle (Groovy DSL) · Spring Security · Micrometer / Actuator.
