# Mobile Application (`:mobile`)

> Клиентская часть проекта «Бюджетный нутрициолог». Включает кроссплатформенное ядро `:mobile:shared` (Kotlin Multiplatform) и тонкий нативный UI `:mobile:androidApp` (Jetpack Compose Material 3), спроектированный с готовностью к подключению iOS-клиента.

---

## 1. Локальная база данных: Room KMP 2.8.x

- **Движок:** `androidx.room:room-runtime:2.8.x` + `androidx.sqlite:sqlite-bundled:2.5.x` (чистый Bundled SQLite, работающий в `commonMain`, Android и iOS).
- **Паттерн:** Single Source of Truth (SSOT). UI слушает локальную базу Room через реактивный `Flow`, а фоновые воркеры и UseCase асинхронно обновляют её из сети.

### Схема сущностей Room

#### 1. Таблица `profiles` (`ProfileEntity.kt`)
Локальный кэш профиля пользователя. Полностью совпадает с `profile.proto` и серверной таблицей `profiles`.

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
Локальный журнал питания с поддержкой Offline-first режима.

```kotlin
@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,     // генерируется 1 раз на клиенте при создании записи
    val foodItemId: String,
    val foodName: String,
    val quantityGrams: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val price: Double,
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

## 3. Offline-First механизм синхронизации

```
Пользователь нажал «Добавить приём пищи»
             │
             ▼
    Генерация IdempotencyKey (UUID v4)
             │
             ▼
   Запись в Room SQLite (status = PENDING)
             │
             ▼
   UI обновляется мгновенно (0 мс задержка)
             │
             ▼
   SyncDiaryWorker (WorkManager)
             │
   Отправка gRPC AddEntry(idempotency-key: <key>)
             │
   ┌─────────┴─────────┐
   ▼                   ▼
Успех                Ошибка
status=SYNCED       status=FAILED
                    BackoffPolicy.EXPONENTIAL
                    (тот же IdempotencyKey!)
```

---

## 4. Архитектура презентации: KoTEA (The Elm Architecture)

Слой представления в `:mobile:shared` построен на KoTEA:
- **`Store<State, UiEvent, News>`**: единая точка входа.
- **`State`**: неделимый снимок состояния с обязательным вложенным `sealed interface Content` (`Loading`, `Success`, `Error`, `Form`). Запрещены флаги `isLoading: Boolean` на верхнем уровне.
- **`DslUpdate`**: чистый синхронный редьюсер без побочных эффектов с исчерпывающим `when` без ветки `else`.
- **`CommandsFlowHandler`**: асинхронный обработчик команд (Room/gRPC).
- **`UiStateMapper`**: маппер чистого состояния в подготовленные для верстки строки (`priceLabel = "185 ₽"`).
- **Compose UI:** разделение на stateful `*Route` (слушает Store, управляет навигацией) и stateless `*Screen` (чистый Composable без бизнес-логики).
