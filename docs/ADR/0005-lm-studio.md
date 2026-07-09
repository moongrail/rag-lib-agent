# ADR 0005 — LM Studio as a local provider

**Status:** Accepted

**Context.** Developers want a fully local option (no cloud key, no egress) for both
embeddings and chat during development and on air-gapped machines.

**Decision.** LM Studio exposes an OpenAI-compatible API (`http://127.0.0.1:1234/v1`).
We support it as:
- chat provider `lmstudio` → `OpenAiChatModel` with LM Studio base URL;
- embedding provider `lmstudio` → `OpenAiEmbeddingModel` with LM Studio base URL.

**Consequences.**
- Reuses the existing OpenAI-compatible client; no new dependency.
- Default model `omnicoder-qwen3.5-9b-claude-4.6-opus-uncensored-v2` (configurable).
- Requires LM Studio running locally with the model loaded; not started by docker-compose.
