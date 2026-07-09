# Конфигурация и переключение провайдеров

Вся конфигурация находится в namespace `app.*` и привязана к
`com.ragassistant.config.AppProperties` через `@ConfigurationProperties`.

## Верхний уровень

| Ключ | По умолчанию | Описание |
|------|--------------|----------|
| `app.tenant.header-name` | `X-Tenant-Id` | HTTP-заголовок с идентификатором тенанта |
| `app.tenant.default` | `default` | тенант, если заголовок отсутствует |
| `app.storage.path` | `./data/uploads` | где хранятся загруженные оригиналы |
| `app.security.enabled` | `false` | включить HTTP Basic auth (демо) |
| `app.security.username` | `rag` | пользователь basic-auth |
| `app.security.password` | `rag` | пароль (используйте `${APP_SECURITY_PASSWORD}`) |
| `app.retrieval.max-results` | `5` | сколько чанков возвращать |
| `app.retrieval.min-score` | `0.0` | порог оценки (RRF ранговый — держите низким) |
| `app.retrieval.rrf-k` | `60` | чувствительность RRF (меньше = акцент на топ) |
| `app.ingestion.chunk-size` | `1000` | токенов в чанке |
| `app.ingestion.chunk-overlap` | `200` | перекрытие токенов |

## Провайдер эмбеддингов — `app.embedding.provider`

`ollama | huggingface | openai | lmstudio | local`

```yaml
app:
  embedding:
    provider: local            # <-- переключение здесь
    ollama:      { base-url: http://localhost:11434, model-name: nomic-embed-text }
    huggingface: { base-url: http://localhost:8082, model-name: BAAI/bge-m3 }
    openai:      { base-url: https://api.openai.com/v1, api-key: ${EMBEDDING_API_KEY}, model-name: text-embedding-3-small }
    lmstudio:    { base-url: http://127.0.0.1:1234/v1, api-key: lm-studio, model-name: omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2 }
```

> **Важна размерность.** Таблица pgvector создаётся с размерностью активной модели
> эмбеддингов. Смена провайдера (и размерности) требует пересоздания таблицы
> `rag_embeddings` (или свежей БД).

## Провайдер чата — `app.chat.provider`

`openrouter | lmstudio`

```yaml
app:
  chat:
    provider: openrouter       # <-- переключение здесь
    openrouter:
      base-url: https://openrouter.ai/api/v1
      api-key:  ${OPENROUTER_API_KEY}
      model-name: cohere/north-mini-code:free
      temperature: 0.2
      max-tokens: 2048
    lmstudio:
      base-url: http://127.0.0.1:1234/v1
      api-key: lm-studio
      model-name: omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2
      temperature: 0.2
      max-tokens: 2048
```

## PostgreSQL / pgvector

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/rag}
    username: ${SPRING_DATASOURCE_USERNAME:rag}
    password: ${SPRING_DATASOURCE_PASSWORD:rag}
```

## Telegram

```yaml
app:
  telegram:
    enabled: true
    token: ${TELEGRAM_BOT_TOKEN}
    allowed-chat-ids: "123456789,987654321"   # пусто = все
```
Активируется профилем `telegram`. Изоляция по чату использует Telegram `chatId` как тенант.

## Профили

| Профиль | Что включает |
|---------|--------------|
| `rest`   | REST-контроллеры `/api/*` + фильтр безопасности |
| `webui`  | Thymeleaf UI на `/` и загрузка `/ui/documents` |
| `telegram` | Telegram-бот (long polling) |
| `cli` | интерактивная консоль (`upload <путь>`, `ask <вопрос>`, `exit`) |

Конфигурация безопасности применяется только под `rest`/`webui`.
