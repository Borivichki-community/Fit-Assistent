# Diary Service (`:backend:diary-service`)

> Серверное хранение и синхронизация записей дневника. Стоимость порции, КБЖУ и остатки рассчитывает мобильный доменный слой.

> **Статус:** Diary RPC, хранение записей, идемпотентность и автоматическая публикация outbox реализованы. SQL/Proto-примеры ниже справочные; канонические контракты находятся в `proto/`, схема БД — в Liquibase migrations. Действующая доставка событий описана в разделе 5.

---

## 1. База данных и схема (`diary_db`)

- **СУБД:** PostgreSQL 17 (порт `5432`, база `diary_db`).
- **Идемпотентность:** постоянная уникальность `(user_id, idempotency_key)` в БД; Redis может ускорять чтение, но не заменяет эту гарантию.
- **Инструмент миграций:** Liquibase (`src/main/resources/db/changelog/db.changelog-master.yaml`).

### Схема таблицы `diary_entries` (`001-diary-schema.xml`)

```sql
CREATE TABLE diary_entries (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,  -- защита от дублирования при retry
    food_item_id UUID NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    quantity_grams NUMERIC(10, 3) NOT NULL,
    calories NUMERIC(7, 2) NOT NULL,
    proteins NUMERIC(6, 2) NOT NULL,
    fats NUMERIC(6, 2) NOT NULL,
    carbs NUMERIC(6, 2) NOT NULL,
    price NUMERIC(10, 2),                         -- стоимость порции в рублях
    unit_price_minor BIGINT,
    price_basis_milligrams BIGINT,
    meal_type VARCHAR(20) NOT NULL,               -- BREAKFAST, LUNCH, DINNER, SNACK
    recorded_at TIMESTAMPTZ NOT NULL,             -- дата и время приёма пищи
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_diary_request UNIQUE(user_id, idempotency_key),
    CONSTRAINT ck_diary_price CHECK (price IS NULL OR price >= 0),
    CONSTRAINT ck_diary_price_basis CHECK (
        (unit_price_minor IS NULL AND price_basis_milligrams IS NULL) OR
        (unit_price_minor IS NOT NULL AND price_basis_milligrams IS NOT NULL
         AND unit_price_minor >= 0 AND price_basis_milligrams > 0 AND price IS NOT NULL)
    )
);

CREATE INDEX idx_diary_entries_user_date ON diary_entries(user_id, recorded_at DESC);
```

### Схема таблицы `outbox_messages` (`002-outbox.xml`)

```sql
CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,          -- 'DiaryEntry'
    aggregate_id VARCHAR(64) NOT NULL,
    topic VARCHAR(100) NOT NULL,                  -- 'diary.entry.created'
    payload BYTEA NOT NULL,                       -- бинарная Avro-сериализация CaloriesConsumedEvent
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pending ON outbox_messages(status, created_at);
```

---

## 2. JPA Сущности

- **`DiaryEntryEntity`**: запись в журнале приёма пищи (КБЖУ, граммовка, цена, идемпотентный ключ).
- **`OutboxMessageEntity`**: сущность Transactional Outbox для надёжной публикации в Kafka вне транзакции БД.

### 2.1 Архитектурное правило JPA: Ленивые связи (LAZY), EntityGraph и защита от N+1

#### 1. Теория: Проблема N+1 при выборке приёмов пищи
При запросе суточного рациона (`GetDailyEntries`):
* Пользователь съедает за день 15–20 продуктов (`food_items`), разделенных по типам приёма пищи (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`).
* Если сущность записи дневника (`DiaryEntryEntity`) имеет связи на справочные сущности (категории, теги, составные рецепты) со стратегией по умолчанию или `FetchType.EAGER`:
  Hibernate сначала выполнит **1 запрос** на получение записей дня:
  ```sql
  SELECT * FROM diary_entries WHERE user_id = ? AND recorded_at BETWEEN ? AND ?;
  ```
  А затем для каждой из 20 записей пошлёт отдельный запрос на связанные данные:
  ```sql
  SELECT * FROM related_entities WHERE id = ?; -- 20 отдельных запросов!
  ```
* Это и есть проблема **N+1**: вместо одного сетевого раундтрипа база нагружается десятками мелких запросов.

#### 2. Правило проекта:
* **СТРОГИЙ ЗАПРЕТ НА `FetchType.EAGER`:** Любые ассоциации в JPA (`@ManyToOne`, `@OneToMany`, `@ManyToMany`) обязаны объявляться исключительно как `fetch = FetchType.LAZY`.
* **Всегда обособленная выборка через `@EntityGraph` или `JOIN FETCH`:** Если бизнес-метод требует сущность вместе с её связями, этот граф объявляется явно в репозитории:

```kotlin
@Entity
@Table(name = "diary_entries")
@NamedEntityGraph(
    name = "DiaryEntry.withDetails",
    attributeNodes = [
        NamedAttributeNode("tags"),
    ]
)
class DiaryEntryEntity(
    @Id val id: UUID,
    val userId: String,
    // ...
    @OneToMany(mappedBy = "entry", fetch = FetchType.LAZY) // СТРОГО LAZY
    val tags: List<DiaryEntryTagEntity> = emptyList(),
)

interface DiaryJpaRepository : JpaRepository<DiaryEntryEntity, UUID> {

    // Загрузка дневника за день за 1 SQL-запрос с JOIN вместо N+1
    @EntityGraph(value = "DiaryEntry.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    fun findByUserIdAndRecordedAtBetween(
        userId: String,
        from: Instant,
        to: Instant,
    ): List<DiaryEntryEntity>
}
```

---

## 3. Protobuf Контракт

- **Файл контракта:** `proto/nutritionist/diary/v1/diary.proto`
- **Пакет:** `com.budgetntr.diary.v1` (Java/Kotlin: `com.budgetntr.proto.diary.v1`)

```protobuf
syntax = "proto3";

package com.budgetntr.diary.v1;

import "google/protobuf/timestamp.proto";

service DiaryService {
  // AddEntry создает новую запись в дневнике питания с проверкой idempotency-key
  rpc AddEntry(AddEntryRequest) returns (AddEntryResponse);

  // GetDailyEntries возвращает все приёмы пищи за конкретную дату и суммарный баланс
  rpc GetDailyEntries(GetDailyEntriesRequest) returns (GetDailyEntriesResponse);
}

message Money {
  int64 amount_minor = 1;
  string currency_code = 2;
}

message CostSnapshot {
  Money total = 1;
  Money unit_price = 2;
  optional int64 price_basis_milligrams = 3;
}

message NutritionSummary {
  int32 calories = 1;
  double proteins = 2;
  double fats = 3;
  double carbs = 4;
}

message AddEntryRequest {
  string food_item_id = 1;
  double quantity_grams = 2;
  google.protobuf.Timestamp recorded_at = 3;
  string meal_type = 4; // BREAKFAST, LUNCH, DINNER, SNACK
  string entry_id = 5;
  string food_name = 6;
  NutritionSummary nutrition = 7;
  CostSnapshot cost = 8;
}

message AddEntryResponse {
  DiaryEntryProto entry = 1;
}

message GetDailyEntriesRequest {
  string date = 1; // ISO-8601: "2026-11-11"
  string time_zone = 2;
}

message GetDailyEntriesResponse {
  repeated DiaryEntryProto entries = 1;
  NutritionSummary daily_total = 2;
}

message DiaryEntryProto {
  string id = 1;
  string food_item_id = 2;
  string food_name = 3;
  double quantity_grams = 4;
  NutritionSummary nutrition = 5;
  google.protobuf.Timestamp recorded_at = 6;
  string meal_type = 7;
  CostSnapshot cost = 8;
}
```

---

## 4. Снимок записи и синхронизация

- Клиент генерирует UUID `entry_id` и `idempotency-key` один раз, рассчитывает порцию и сохраняет весь payload в Room до сети. Запись принадлежит проверенному субъекту авторизации; тело запроса не выбирает другого пользователя.
- `cost` отсутствует, если стоимость неизвестна. Если стоимость известна, `total` обязателен. Нулевой total — известная нулевая стоимость. `unit_price` и положительный `price_basis_milligrams` присутствуют вместе, если сохранена исходная цена. Для ручной итоговой суммы они могут отсутствовать.
- MVP использует только RUB. API передаёт копейки через `int64`, сервер точно переводит их в `price NUMERIC(10,2)` делением на 100. Это смена представления, не пересчёт порции. В БД валюта не дублируется, пока иные валюты запрещены. Исходная цена и её база необязательны; количество уже хранится в записи.
- Сервер проверяет владельца, UUID, допустимые значения meal_type, дату/часовой пояс, конечность и диапазоны чисел, положительное количество с точностью до 0.001 г, неотрицательные КБЖУ и деньги, валюту и согласованность присутствия полей. Для `price NUMERIC(10,2)` максимум — 9 999 999 999 копеек. Проверка не обращается к текущим ценам и не пересчитывает клиентскую сумму.
- Это пользовательская оценка расходов на потреблённую порцию, а не подтверждённая оплата/чек. Серверное подтверждение означает сохранение, не подтверждение рыночной цены.
- Повтор `(subject, idempotency-key)` с тем же payload возвращает прежнюю запись; другой payload — `INVALID_ARGUMENT`. PK записи и ключ проверяются атомарно; конкурентный повтор не создаёт вторую запись или outbox-событие. При попытке повторно создать тот же `entry_id` с новым ключом возвращается конфликт, клиент не генерирует новый ID автоматически.
- Историческая стоимость не меняется при обновлении каталога. Для исправления пользователем потребуется отдельный будущий сценарий редактирования; данный AddEntry не является upsert.
- `GetDailyEntries` использует IANA time_zone для границ локального дня, возвращает сохранённые снимки и сумму сохранённых нутриентов. Клиент считает денежный остаток из объединения серверных записей и локальных PENDING по `entry_id`, без двойного учёта. Для недельного остатка загружаются все дни недели; неполный кэш явно помечается в UI.

## 5. Межсервисные события

T-052: действующий AddEntry сохраняет запись питания, событие в outbox_messages и ответ идемпотентности в одной транзакции PostgreSQL. Координация находится в DiaryService, отдельный проходной UseCase не вводится. Outbox содержит локально сериализованный Avro CaloriesConsumedEvent со статусом PENDING, aggregate_id=entry_id, partition_key=userId и входящим traceparent. Повтор возвращает сохранённый ответ, не создавая нового события. Ошибка записи события или формирования ответа откатывает всю операцию. Сохранённый контракт Avro требует UUID пользователя.

Миграция 004-diary-outbox.yaml не пересекается с 003-diary-registration-consumer.yaml из отдельной ветки T-055. В транзакции добавления нет Kafka/HTTP/gRPC-вызовов.

T-053: общий JVM-модуль `:backend:outbox` содержит контракт сообщения, JDBC store, настройки и процессор. Diary реализует публикатор `CaloriesConsumedEvent` и запускает процессор через Spring `@Scheduled`. Бинарный Avro из outbox восстанавливается в объект; `KafkaAvroSerializer` добавляет Confluent-обрамление/schema ID вне транзакции БД. Schema Registry остаётся в режиме BACKWARD.

### Порядок и защита нескольких экземпляров

- `partition_key` и Kafka key — каноническая строка UUID пользователя в нижнем регистре. Публикатор проверяет UUID по значению и передаёт сохранённый `traceparent` в заголовке.
- Append захватывает transaction advisory lock пользователя перед выделением `sequence_number`. Следующая запись этого пользователя получает номер только после commit/rollback предыдущей. Время приёма пищи и `created_at` не определяют порядок публикации.
- Один активный отправитель на `diary_db` удерживает session advisory lock на выделенном autocommit-соединении. Это не транзакция БД. Другой экземпляр пропускает цикл, если блокировка занята; блокировка явно снимается перед возвратом соединения в пул. При ошибке снятия соединение принудительно закрывается.
- Каждый цикл выбирает только первое неопубликованное событие для пары topic/key. Пока оно ожидает retry или находится в FAILED, следующие события пользователя не отправляются; другие пользователи могут обрабатываться.
- Kafka producer использует транзакции, `acks=all`, идемпотентность и один запрос в полёте. В начале непустой порции producer пересоздаётся с тем же `transactional.id`: новый владелец блокировки отсекает старого producer, если тот потерял PostgreSQL-соединение. После init/begin Kafka-транзакции, непосредственно перед send, публикатор вызывает проверку исходного DB-соединения и точного advisory lock через pg_locks. Поэтому старый владелец, возобновившийся даже до своего init, не отправит устаревшее событие после takeover. Транзакция Kafka выполняется вне транзакции БД.
- Все реплики одной очереди должны использовать одинаковый `DIARY_OUTBOX_TRANSACTION_PREFIX` (по умолчанию `diary-outbox-`). Для независимых очередей/окружений в одном Kafka-кластере префиксы должны различаться. Кэш suffix ограничен одним producer, поэтому ID стабилен между владельцами.
- Потребители должны использовать `isolation.level=read_committed`: незавершённые/отменённые транзакции старого владельца не видны. Число партиций topic должно оставаться неизменным для сохранения привязки пользователя.

### Подтверждение, повторы и восстановление

После успешного commit транзакции Kafka процессор отдельным autocommit UPDATE выставляет PUBLISHED и `published_at`. До подтверждения строка остаётся PENDING. При ошибке увеличиваются `attempts`, записываются тип ошибки и `next_attempt_at`; payload, ID, ключ и traceparent не меняются. Backoff экспоненциальный с верхним пределом.

Доставка — at least once. Если Kafka приняла событие, но процесс остановился до UPDATE, новый обработчик отправит тот же факт повторно перед последующими событиями пользователя. Потребитель дедуплицирует создание по (userId, mealId); это не exactly once между PostgreSQL и Kafka.

Настройки `diary.outbox.*`: enabled=true, poll-interval=PT1S, batch-size=100, max-attempts=10, initial-backoff=PT1S, max-backoff=PT5M. Длительности задаются в ISO-8601. `DIARY_OUTBOX_ENABLED=false` выключает расписание. JDBC pool должен поддерживать autocommit и иметь свободное соединение для обработчика. В настройках producer ограничены ожидания metadata/отправки; при выключении процессор не освобождает блокировку, пока текущая отправка не завершилась.

После max-attempts строка остаётся в FAILED и блокирует пользователя. Ошибка видна в логах и колонках status/attempts/last_error. После устранения причины можно повторить конкретную строку через `JdbcOutboxStore.retryFailed(OutboxMessageId)`: операция под блокировкой сбрасывает attempts и возвращает PENDING, сохраняя sequence/payload. Метод возвращает false, если строка не FAILED либо очередь сейчас занята. Операция администрирования вручную:

```sql
UPDATE outbox_messages
SET status = 'PENDING', attempts = 0,
    next_attempt_at = CURRENT_TIMESTAMP, last_error = NULL
WHERE id = :message_id AND status = 'FAILED';
```

### Миграция и поставка

Миграция `005-diary-outbox-delivery.yaml` добавляет очередь, retry и индексы, нормализует существующие UUID-ключи. Старый backlog получает детерминированный порядок (created_at, id): восстановить неизвестный прежний порядок commit задним числом невозможно. При первом обновлении нужно остановить старые writers/publishers, применить миграцию и запустить новую версию; смешанный запуск старого writer без advisory lock с новым не поддерживается.

Dockerfile и .dockerignore включают backend/outbox, shared/contracts и proto. Сборка: `docker build -f infrastructure/docker/Dockerfile.service --build-arg SERVICE_NAME=diary-service -t fitassistant/diary-service:t053 .`. Это локальная сборка без публикации.

Существующая [CaloriesConsumedEvent](../../shared/contracts/src/main/avro/CaloriesConsumedEvent.avsc) содержит `userId`, `mealId`, `calories`, `timestamp`, `mealType`, `notes`. Её целевое назначение — факт потребления, а не денежный расход. При будущей публикации `mealId = entry_id`, `timestamp = recorded_at`, `calories` берётся из сохранённой записи; доставка через outbox. Потребитель дедуплицирует создание по `(userId, mealId)`.

`diary.entry.created` не используется Recipe AI для ведения баланса. Поле стоимости в Avro не добавляется; новый обязательный потребитель не вводится. Дневник не потребляет `food.price.updated`: цены каталога не меняют исторические снимки.

### 5.1 Входящая регистрация пользователя — T-055 / KAN-73

Diary принимает `UserRegisteredEvent` из `user.registered` в группе `diary-service`. Формат совпадает с отправителем Profile из `develop` (`c86dfe0`); изменения Avro-схемы не требуются.

Инициализация создаёт пустой дневник: строку `diary_users` с `user_id`, `registered_at` и `initialized_at`. Email и источник регистрации не копируются, фиктивные записи еды не создаются. Существующие записи питания сохраняются; gRPC не блокируется в ожидании события регистрации.

В одной PostgreSQL-транзакции сохраняются семантический ключ `UserRegisteredEvent:<userId>` в `processed_events` и строка дневника. В текущем контракте нет отдельного eventId: регистрация одного пользователя считается одним фактом, даже если повтор пришёл с другим Kafka offset. Дедупликация постоянная; TTL ответов gRPC к ней не относится.

Consumer подтверждает offset вручную только после возврата транзакционного сервиса, то есть после коммита. По умолчанию выполняются первоначальная попытка и два retry с интервалом 1 секунда. Ошибки валидации и десериализации не повторяются. После исчерпания retry сообщение отправляется в `user.registered.DLT`; offset подтверждается только при успешной отправке в DLT. При ошибке десериализации сохраняются исходные байты.

Настройки находятся в `diary.kafka.registration`: `enabled`, `topic`, `partitions`, `replication-factor`, `retry-interval`, `retries`. По умолчанию основной топик и DLT создаются с тремя партициями и replication factor 1 для локального стенда. Адреса брокера и Schema Registry задаются существующими `KAFKA_BOOTSTRAP_SERVERS` и `SCHEMA_REGISTRY_URL`. Для отключения consumer можно задать `DIARY_REGISTRATION_CONSUMER_ENABLED=false`.

Интеграционные тесты используют отдельные Testcontainers PostgreSQL, Kafka и Schema Registry; рабочий Compose-стенд не используется.
