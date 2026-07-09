# ADR 0003 — OpenRouter через OpenAI-совместимый API

**Статус:** Принято

**Контекст.** OpenRouter — единый шлюз к сотням моделей, но не имеет собственного модуля
LangChain4j и не отдаёт эмбеддинги. При этом он полностью совместим с OpenAI API.

**Решение.** Использовать `dev.langchain4j.model.openai.OpenAiChatModel`, направленный на
`https://openrouter.ai/api/v1` с ключом OpenRouter и slug модели (напр.
`cohere/north-mini-code:free`). Тот же OpenAI-клиент обслуживает внешнего провайдера
эмбеддингов (`app.embedding.openai`), когда выбран «другой провайдер».

**Последствия.**
- Одна интеграция покрывает чат OpenRouter и любой OpenAI-совместимый эндпоинт эмбеддингов.
- Простая смена модели через `app.chat.openrouter.model-name`.
- Компромисс: зависим от семантики OpenAI-клиента; приемлемо given совместимость.
