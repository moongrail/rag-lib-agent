# Справочник API

Базовый URL: `http://localhost:8080`. Всё в JSON. Тенант берётся из заголовка `X-Tenant-Id`
(по умолчанию `default`). При `app.security.enabled=true` передавайте HTTP Basic.

## Документы

### `POST /api/documents` (multipart/form-data)

Загрузка и индексация одного документа.

- Поле формы: `file` (PDF / DOCX / TXT / MD / XML)
- 200 → `UploadResponse`

```json
{ "id": "3fa85f64-...", "fileName": "manual.pdf", "status": "INGESTED" }
```

`status` — `INGESTED` или `FAILED` (сбои фиксируются, не бросаются).

### `POST /api/documents/bulk` (multipart/form-data)

Загрузка и индексация **нескольких документов сразу** (параллельно, virtual threads).

- Поле формы: `files` (один или несколько; PDF / DOCX / TXT / MD / XML)
- 200 → `List<UploadResponse>` (по одной записи на файл; сбой одного не прерывает пачку)

```json
[
  { "id": "3fa85f64-...", "fileName": "a.pdf", "status": "INGESTED" },
  { "id": "9bb2c1aa-...", "fileName": "b.xml", "status": "INGESTED" },
  { "id": null, "fileName": "bad.xyz", "status": "FAILED" }
]
```

### `GET /api/documents`

Список документов текущего тенанта.

```json
[
  { "id": "3fa85f64-...", "title": "manual.pdf", "fileName": "manual.pdf",
    "status": "INGESTED", "chunkCount": 42, "createdAt": "2026-07-10T12:00:00Z" }
]
```

### `GET /api/documents/{id}`

Сводка документа текущего тенанта (404, если не найден / чужой тенант).

## Чат

### `POST /api/chat`

```json
{ "message": "What is the refund policy?", "sessionId": "опционально" }
```

- `sessionId` необязателен; если не указан, генерируется новый UUID и возвращается.
- 200 → `ChatResponse`

```json
{
  "sessionId": "sess-1",
  "answer": "According to the manual, refunds are processed within 14 days...",
  "citations": [
    { "fileName": "manual.pdf", "page": null, "snippet": "Refunds...", "score": 0.031 }
  ]
}
```

## Web UI

При профиле `webui`:
- `GET /` — страница чата (`templates/chat.html`).
- `POST /ui/documents` (multipart, поле `file`) — загрузка с редиректом на `/`.

## Telegram

При профиле `telegram` и заданном `app.telegram.token` бот отвечает на любое текстовое
сообщение через RAG-конвейер. Telegram `chatId` используется как тенант для изоляции.
Опционально `app.telegram.allowed-chat-ids` ограничивает допущенных собеседников.

## CLI

При профиле `cli`:

```
RAG Assistant CLI. Commands: 'upload <path>', 'ask <question>', 'exit'
> upload ./manual.pdf
Ingested: ./manual.pdf
> ask What is the refund policy?
...
```
