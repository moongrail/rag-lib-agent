# RAG-конвейер

## Загрузка (индексация)

1. **Получение** — `DocumentIngestionService.ingest(...)` принимает байты из загрузки или пути.
2. **Сохранение оригинала** — байты пишутся в `app.storage.path/<tenant>/<uuid>_<имя>`
   (для повтора загрузки и аудита).
3. **Парсинг**
   - PDF → `ApachePdfBoxDocumentParser`
   - остальное (DOCX, TXT, MD) → `ApacheTikaDocumentParser`
4. **Разбиение** — `DocumentSplitters.recursive(chunkSize, overlap)` (рекурсивно по токенам).
   По умолчанию: 1000 токенов / 200 перекрытия.
5. **Обогащение метаданными** — каждый `TextSegment` получает `tenantId`, `fileName`, `docId`.
6. **Эмбеддинг** — `EmbeddingProvider.embedAll(segments)` → `List<Embedding>`.
7. **Сохранение** — `embeddingStore.add(embedding, segment)` в pgvector (`rag_embeddings`).
8. **Фиксация** — `DocumentMetadata` сохраняется со `status=INGESTED`, `chunkCount`. При
   любой ошибке статус становится `FAILED` с `error_message`, файл удаляется.

## Поиск (online)

1. **Эмбеддинг запроса** — `EmbeddingProvider.embed(question)`.
2. **Гибридный поиск** — `PgVectorEmbeddingStore.search(...)` с `SearchMode.HYBRID`:
   - **Векторная** часть: косинусное сходство по `embedding`.
   - **Лексическая** часть: PostgreSQL `tsvector` FTS по тексту сегмента.
   - **Слияние**: Reciprocal Rank Fusion `1/(k+rank_vector) + 1/(k+rank_keyword)` (k=`rrf-k`).
   - Гибридный запрос **требует обоих**: `queryEmbedding` и текст `query`.
3. **Изоляция тенанта** — фильтр `metadataKey("tenantId").isEqualTo(tenantId)`, тенант
   видит только свои чанки.
4. **Результат** — `List<RetrievedChunk>` (текст + метаданные + оценка), ограничен `max-results`.

## Генерация

1. `ChatService` объединяет найденные чанки в блок `Context:`.
2. Сообщения: `System` (инструкции по grounded-ответам) + **история** сессии + `User(контекст+вопрос)`.
3. `ChatModel.chat(messages)` (OpenRouter или LM Studio) возвращает ответ.
4. Пара `(вопрос, ответ)` добавляется в оконную память сессии (последние 20 ходов).
5. Цитаты формируются из найденных чанков: `fileName`, `page` (если есть),
   `snippet` (усечённый), `score`.

## Чек-лист настройки

- **Шумный поиск** → уменьшите `max-results` (3–5), увеличьте `chunk-size` для широкого контекста.
- **Не находятся точные термины** → убедитесь, что `SearchMode.HYBRID` (по умолчанию) и
  `textSearchConfig` соответствует языку (`simple` / `english` / …).
- **Галлюцинации** → системный промпт запрещает отвечать вне контекста; не убирайте его.
- **Задержки при росте** → включите `useIndex(true)` + `indexListSize` в pgvector
  (см. `VectorStoreConfig`) для >100k эмбеддингов.

## Заметки / будущее

- **Постраничные цитаты**: парсер PDF пока хранит метаданные на уровне документа.
  Постраничность можно добавить, парся по страницам и проставляя `page` в метаданных
  сегмента (код поиска/цитат уже читает `page`).
- **Ререйтинг**: cross-encoder можно вставить между поиском и генерацией для точности.
