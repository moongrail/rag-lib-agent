# Разработка

## Сборка и запуск

```bash
./gradlew build                 # компиляция + тесты
./gradlew bootRun               # запуск (профили из SPRING_PROFILES_ACTIVE)
./gradlew bootJar               # сборка build/libs/rag-assistant.jar
java -jar build/libs/rag-assistant.jar
```

## Тесты

`test` использует JUnit 5 и Testcontainers (PostgreSQL/pgvector). In-process провайдер
`local` держит тесты эмбеддингов без внешних сервисов.

```bash
./gradlew test
```

Рекомендуемое покрытие:
- `DocumentIngestionService` — парсинг + разбиение + сохранение (Testcontainers pgvector, `local`).
- `DocumentRetrievalService` — фильтр тенанта возвращает только свои чанки.
- `ChatService` — ответ опирается на переданный контекст (мок `ChatModel`).
- Контроллеры адаптеров — `@WebMvcTest` для DTO и кодов статуса.

## Соглашения по коду

- Java 21, Spring Boot 3.4, Gradle Groovy DSL.
- Слоистые пакеты: `config / domain / embedding / ingestion / retrieval / chat / adapter / common`.
- Интерфейсы (`EmbeddingProvider`) скрывают детали LangChain4j — провайдеры взаимозаменяемы.
- В адаптерах нет бизнес-логики — только маппинг DTO и разрешение тенанта.
- Ошибки → `RagException` (HTTP 400) через `GlobalExceptionHandler` (`ProblemDetail`).
- Логи структурированы; `TenantFilter` кладёт `tenant` в MDC.

## Наблюдаемость

- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.
- Micrometer-трейсинг (Brave). Экспорт спанов в Zipkin настраивается через
  `management.zipkin.tracing.endpoint` (по умолчанию выключен, чтобы не спамить localhost:9411).
- Добавляйте метрики в сервисы через `MeterRegistry` (длительность загрузки, хиты поиска).

## Переключение эмбеддингов/чата в рантайме

Разрешаются при старте из `app.embedding.provider` / `app.chat.provider`. Для смены без
рестарта вынесите решение в `@RefreshScope`-конфиг или перезапустите приложение
(рекомендуется при смене размерности — пересоздайте таблицу векторов).

## Чек-лист для продакшена

- [ ] Секреты в vault / k8s secrets (никогда не коммитьте `.env`).
- [ ] `app.security.enabled=true` и замена Basic-auth на OAuth2 Resource Server.
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (схемой владеет Flyway — уже так).
- [ ] Для больших корпусов включить pgvector `useIndex(true)` + `indexListSize`.
- [ ] Бэкап БД `rag` (векторы + метаданные) и `app.storage.path` загрузок.
- [ ] Ограничения rate limit / размера запроса на `/api/documents` (уже лимит 50MB).
- [ ] Отправка трейсов в коллектор; алерты на сбои загрузки и задержки чата.
