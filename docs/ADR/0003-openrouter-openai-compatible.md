# ADR 0003 — OpenRouter via the OpenAI-compatible API

**Status:** Accepted

**Context.** OpenRouter is a unified gateway to 100s of models but does not provide its own
LangChain4j module, and it does not serve embeddings. It is, however, fully
OpenAI-compatible.

**Decision.** Use `dev.langchain4j.model.openai.OpenAiChatModel` pointed at
`https://openrouter.ai/api/v1` with the OpenRouter key and a model slug
(e.g. `cohere/north-mini-code:free`). The same OpenAI client serves an external embedding
provider (`app.embedding.openai`) when "another provider" is chosen.

**Consequences.**
- One integration covers OpenRouter chat and any OpenAI-compatible embedding endpoint.
- Easy model swap by changing `app.chat.openrouter.model-name`.
- Trade-off: we depend on OpenAI client semantics; acceptable given compatibility guarantee.
