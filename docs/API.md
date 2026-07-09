# API Reference

Base URL: `http://localhost:8080`. All JSON. Tenant is taken from the `X-Tenant-Id` header
(defaults to `default`). When `app.security.enabled=true`, send HTTP Basic credentials.

## Documents

### `POST /api/documents` (multipart/form-data)

Upload and ingest a single document.

- Form field: `file` (PDF / DOCX / TXT / MD / XML)
- 200 → `UploadResponse`

```json
{ "id": "3fa85f64-...", "fileName": "manual.pdf", "status": "INGESTED" }
```

`status` is `INGESTED` or `FAILED` (failures are recorded, not thrown).

### `POST /api/documents/bulk` (multipart/form-data)

Upload and ingest **many documents at once** (parallel, virtual threads).

- Form field: `files` (one or more; PDF / DOCX / TXT / MD / XML)
- 200 → `List<UploadResponse>` (one entry per file; a single failure does not abort the batch)

```json
[
  { "id": "3fa85f64-...", "fileName": "a.pdf", "status": "INGESTED" },
  { "id": "9bb2c1aa-...", "fileName": "b.xml", "status": "INGESTED" },
  { "id": null, "fileName": "bad.xyz", "status": "FAILED" }
]
```

### `GET /api/documents`

List documents for the current tenant.

```json
[
  { "id": "3fa85f64-...", "title": "manual.pdf", "fileName": "manual.pdf",
    "status": "INGESTED", "chunkCount": 42, "createdAt": "2026-07-10T12:00:00Z" }
]
```

### `GET /api/documents/{id}`

Document summary for the current tenant (404 if not found / wrong tenant).

## Chat

### `POST /api/chat`

```json
{ "message": "What is the refund policy?", "sessionId": "optional-session" }
```

- `sessionId` is optional; when omitted a new session UUID is generated and returned.
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

When profile `webui` is active:

- `GET /` → chat page (`templates/chat.html`).
- `POST /ui/documents` (multipart, field `file`) → ingest then redirect to `/`.

## Telegram

When profile `telegram` is active and `app.telegram.token` is set, the bot answers any text
message using the RAG pipeline. The Telegram `chatId` is used as the tenant for isolation.
Optional `app.telegram.allowed-chat-ids` restricts who can talk to the bot.

## CLI

When profile `cli` is active:

```
RAG Assistant CLI. Commands: 'upload <path>', 'ask <question>', 'exit'
> upload ./manual.pdf
Ingested: ./manual.pdf
> ask What is the refund policy?
...
```
