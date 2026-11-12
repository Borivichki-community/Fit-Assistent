# Mobile Application (`:mobile`)

> Клиентская часть проекта «Бюджетный нутрициолог». Включает кроссплатформенное ядро `:mobile:shared` (Kotlin Multiplatform) и тонкий нативный UI `:mobile:androidApp` (Jetpack Compose Material 3), спроектированный с готовностью к подключению iOS-клиента.

---

## 1. Локальная база данных: Room KMP 2.8.x

- **Движок:** `androidx.room:room-runtime:2.8.x` + `androidx.sqlite:sqlite-bundled:2.5.x` (чистый Bundled SQLite, работающий в `commonMain`, Android и iOS).
- **Паттерн:** Single Source of Truth (SSOT). UI слушает локальную базу Room через реактивный `Flow`, а фоновые воркеры и UseCase асинхронно обновляют её из сети.

### Схема сущностей Room

#### 1. Таблица `profiles` (`ProfileEntity.kt`)
Локальный кэш профиля пользователя; сопоставление с существующим Profile Proto выполняет mapper. Серверная таблица пока запланирована.

```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val profileId: String,
    val heightCm: Int,
    val weightKg: Double,
    val gender: String,             // MALE, FEMALE, UNSPECIFIED
    val activityLevel: String,      // SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE, UNSPECIFIED
    val goalType: String,           // LOSE_WEIGHT, MAINTAIN, GAIN_WEIGHT, UNSPECIFIED
    val targetCalories: Int,
    val targetProteinsGrams: Double,
    val targetFatsGrams: Double,
    val targetCarbsGrams: Double,
    val targetWeightKg: Double,
    val weeklyBudget: Double,
    val updatedAtEpochMs: Long,
)
```

#### 2. Таблица `diary_entries` (`DiaryEntryEntity.kt`)
Целевая модель, ещё не подключённая в AppDatabase. Room хранит клиентский снимок, а не пересчитываемую по каталогу историю.

```kotlin
@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val idempotencyKey: String,     // генерируется 1 раз на клиенте при создании записи
    val foodItemId: String,
    val foodName: String,
    val quantityGrams: Double,
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val costMinor: Long?,
    val unitPriceMinor: Long?,
    val priceBasisMilligrams: Long?,
    val mealType: String,
    val recordedAtEpochMs: Long,
    val syncStatus: String,         // "PENDING" | "SYNCED" | "FAILED"
)
```

---

## 2. Сетевой слой и Protobuf-контракты

- **Транспорт:** gRPC через HTTP/2 на Kubernetes Envoy Gateway (порт `9090` / `443`).
- **Авторизация:** Неблокирующий `AuthCallCredentials` на базе `io.grpc.CallCredentials`.
  - OAuth 2.1 Authorization Code + PKCE через библиотеку `AppAuth`;
  - Безопасное хранение токенов в Android Keystore / Tink AES-256;
  - Автоматический refresh токена `refreshIfNeeded()` перед сетевым вызовом без блокировки потоков (`runBlocking` запрещён).
- **Трассировка:** `TraceparentClientInterceptor` внедряет заголовки W3C `traceparent` для распределённой трассировки OpenTelemetry.

---

## 3. Offline-First: стоимость и синхронизация

- Каталог предоставляет справочные цены. Итог порции и остаток недельного лимита рассчитывает `mobile/shared/domain`; Compose получает готовые строки из UiStateMapper.
- MVP использует RUB/копейки. `costMinor=null` — неизвестная стоимость, 0 — известная нулевая. `unitPriceMinor` и положительный `priceBasisMilligrams` сохраняются вместе при известной исходной цене; ручная итоговая сумма может быть без них. Количество — до 0.001 г; точные операции и HALF_UP описаны в [Mobile Stack §3.1](../docs/architecture/03-mobile-stack.md#31-стоимость-и-запись-дневника).
- Room атомарно сохраняет полный снимок и один idempotencyKey до сети. Retry отправляет прежний снимок. Новый каталог не изменяет прошлый расход.
- `Diary.AddEntry` сохраняет клиентскую сумму и проверяет данные; денежный пересчёт на сервере не нужен. ACK переводит запись в SYNCED; временная ошибка сохраняет PENDING и запускает ограниченный backoff; постоянная ошибка/исчерпание попыток сохраняет FAILED для явного повтора. CancellationException пробрасывается.
- При восстановлении серверные записи объединяются с локальными по entry_id, чтобы не удвоить расход. Неполная история периода или неизвестные стоимости помечаются в UI. Recipe AI получает рассчитанные ограничения конкретного запроса, не строит остаток по Kafka.

### Избранное

Единый целевой RPC — `Catalog.SetFavorite(target, is_favorite)` для FOOD_ITEM, RECIPE, DAILY_MEAL_PLAN. Схема локального хранения:

| Таблица | Назначение |
|---|---|
| favorite_items | Желаемое состояние по (userId, targetType, targetId), lastOperationId и данные отображения; false хранится как tombstone до подтверждения |
| favorite_operations | Неизменяемый payload, operationId (= ключ), sequence, владелец, статус, attempts и код ошибки |

Обновление UI и вставка операции атомарны. Worker отправляет FIFO одного аккаунта; старый ACK удаляет только свою операцию и не меняет более новое состояние. После шести неудачных отправок автоматическая работа останавливается, данные сохраняются. Вход/явный повтор возобновляет очередь, сохраняя ключ неизвестного исхода. Подробные состояния, восстановление и пример worker — [Mobile Stack §3](../docs/architecture/03-mobile-stack.md#3-offline-first-дневник-и-избранное).

Это план реализации: существующая Room БД содержит только profiles. Новые таблицы и workers данным изменением не создаются.

---

## 4. Архитектура презентации: KoTEA (The Elm Architecture)

Слой представления в `:mobile:shared` построен на KoTEA:
- **`Store<State, UiEvent, News>`**: единая точка входа.
- **`State`**: неделимый снимок состояния с обязательным вложенным `sealed interface Content` (`Loading`, `Success`, `Error`, `Form`). Запрещены флаги `isLoading: Boolean` на верхнем уровне.
- **`DslUpdate`**: чистый синхронный редьюсер без побочных эффектов с исчерпывающим `when` без ветки `else`.
- **`CommandsFlowHandler`**: асинхронный обработчик команд (Room/gRPC).
- **`UiStateMapper`**: маппер чистого состояния в подготовленные для верстки строки (`priceLabel = "185 ₽"`).
- **Compose UI:** разделение на stateful `*Route` (слушает Store, управляет навигацией) и stateless `*Screen` (чистый Composable без бизнес-логики).
