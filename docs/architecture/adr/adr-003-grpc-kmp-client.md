# ADR-003: Архитектура gRPC-клиента в Kotlin Multiplatform (Unary, Server-Streaming и стратегия готовности к iOS)

* **Статус**: Accepted (Принято)
* **Дата**: 2026-11-08
* **Контекст задачи**: [T-014] Spike / Архитектура gRPC-клиента в KMP (Android + iOS)
* **Авторы**: `implementer` (архитектура KMP и gRPC), `test-reviewer` (аудит безопасности потоков и корутин)

---

## 1. Контекст и проблематика (Context)

В проекте «Бюджетный нутрициолог» (FitAssistent2) взаимодействие между мобильным клиентом (:mobile:shared, :mobile:androidApp, в перспективе iOS) и серверными микросервисами (Spring Boot 4.1) строится по протоколу **gRPC / HTTP/2** на базе Protobuf контрактов (`proto/nutritionist/*`).

### Типы сетевых взаимодействий:
1. **Unary RPC (запрос-ответ):**
   - Получение и обновление профиля (`ProfileService.GetProfile`, `ProfileService.UpdateProfile`);
   - Фиксация записей дневника питания (`DiaryService.AddDiaryEntry`);
   - Запрос цен продуктов в каталоге (`CatalogService.GetProductPrices`).
2. **Server-Streaming RPC (один запрос — поток ответов):**
   - Потоковая генерация и отдача рациона LLM (`RecipeService.StreamDietPlan` / `GenerateRecipeStream`) по мере генерации токенов моделью;
   - Стриминг обновлений каталога или статуса пакетной синхронизации дневника питания.

### Проблема кроссплатформенности (KMP Challenge):
Официальный стек Google gRPC (`grpc-java`, `grpc-kotlin`, `grpc-okhttp`) построен на базе Java JVM и не компилируется под **Kotlin/Native** (`iosArm64`, `iosSimulatorArm64`). Прямое подключение `grpc-kotlin` в `commonMain` невозможно, так как это нарушит сборку под iOS и разрушит правило чистоты domain-слоя (§2.2 [AGENTS.md](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/AGENTS.md)).

---

## 2. Факторы принятия решений (Decision Drivers)

1. **Абсолютная чистота `commonMain` (§2.2, §2.4 AGENTS.md):**
   - В `commonMain` категорически запрещены импорты `io.grpc.*`, `com.google.protobuf.*`, `java.*`, `android.*`.
   - Домен и репозитории оперируют только чистыми доменными моделями (`Profile`, `Recipe`, `DiaryEntry`) и типизированными идентификаторами `@JvmInline value class` (`UserId`, `RecipeId`).
2. **Поддержка Unary и Server-Streaming вызовов через Kotlin Coroutines:**
   - Unary методы возвращают `Result<T>` (`suspend fun invoke(): Result<T>`);
   - Server-Streaming методы возвращают `Flow<T>`, поддерживающий отмену (cancellation) и противодавление (backpressure).
3. **Безопасность потоков и авторизация OAuth 2.1 (Zero `runBlocking`):**
   - Добавление токена `Bearer <token>` и W3C `traceparent` должно происходить асинхронно без блокировки потоков (`AuthCallCredentials`).
4. **Готовность к iOS (iOS Readiness):**
   - Архитектура должна позволить подключить iOS-таргет без переписывания доменной логики, UseCase, репозиториев или MVI-компонентов KoTEA.

---

## 3. Рассмотренные варианты интеграции gRPC в KMP (Considered Options)

### Вариант 1: Square Wire (KMP Protobuf & gRPC)
* **Суть**: Использование библиотеки Wire от Square вместо стандартного Google `protoc`. Wire компилирует `.proto` в чистый Kotlin Multiplatform код, работающий поверх Okio и Ktor Client / HTTP/2.
* **Плюсы**:
  * Единая кодовая база стабов и DTO прямо в `commonMain`.
* **Минусы**:
  * ❌ Расхождение с Google Buf и серверным плагином `protoc-gen-grpc-kotlin`;
  * ❌ Сложнее настраивать кастомные интерцепторы и генерацию Java/Kotlin для Spring Boot бэкенда;
  * ❌ Ограниченная поддержка некоторых расширенных возможностей gRPC (динамический compression, advanced name resolution).

### Вариант 2: gRPC C-Core через Kotlin/Native `cinterop`
* **Суть**: Компиляция нативной C-библиотеки gRPC под iOS и генерация обертки через `cinterop`.
* **Плюсы**:
  * Использование оригинального C++ ядра gRPC.
* **Минусы**:
  * ❌ Огромный размер итогового бинарника приложения (+30..50 МБ из-за OpenSSL/BoringSSL);
  * ❌ Крайне сложная поддержка сборки в Gradle/Xcode и высокий риск сбоев линковки.

### Вариант 3: Архитектурная изоляция интерфейсом RemoteDataSource в `commonMain` + Платформенные реализации (Выбранный вариант)
* **Суть**:
  - В `commonMain` объявляются чистые интерфейсы источников данных (`ProfileRemoteDataSource`, `RecipeRemoteDataSource`).
  - Для Unary RPC методы возвращают `suspend fun ... : Result<DomainModel>`.
  - Для Server-Streaming RPC методы возвращают `fun ... : Flow<DomainModel>`.
  - В `androidMain`: реализация через официальный `grpc-kotlin` + `grpc-okhttp` + `AuthCallCredentials`.
  - В `iosMain`: реализация интерфейса через Koin DI с вызовом `grpc-swift` (Apple Swift gRPC) или нативного URLSession HTTP/2 шлюза.
* **Плюсы**:
  * ✅ 100% соблюдение Clean Architecture и правил AGENTS.md;
  * ✅ `commonMain` не имеет ни одной зависимости от gRPC библиотек;
  * ✅ Полная поддержка корутин: `suspend fun` для Unary и `Flow<T>` для Streaming;
  * ✅ Независимая эволюция сетевого транспорта на Android и iOS;
  * ✅ Тривиальное тестирование в `commonTest` с помощью `Fake*RemoteDataSource` без моков и без запуска gRPC-сервера.

---

## 4. Решение (Decision Outcome)

Принять **Вариант 3: Изоляция за интерфейсом RemoteDataSource в `commonMain`**.

### 4.1 Паттерн для Unary RPC
```kotlin
// commonMain: dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
interface ProfileRemoteDataSource {
    suspend fun getProfile(userId: UserId): Result<Profile>
    suspend fun updateProfile(profile: Profile): Result<Profile>
}

// commonMain: dev.andre.nutritionist.data.remote.grpc.ProfileGrpcRemoteDataSource
class ProfileGrpcRemoteDataSource(
    private val client: ProfileGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
) : ProfileRemoteDataSource {

    override suspend fun getProfile(userId: UserId): Result<Profile> =
        withContext(dispatchers.io) {
            runCatching {
                val request = GetProfileRequest(userId = userId.raw)
                val response = client.getProfile(request)
                response.toDomain()
            }.onFailure { if (it is CancellationException) throw it }
        }
}
```

### 4.2 Паттерн для Server-Streaming RPC
В `grpc-kotlin` серверный стриминг генерирует функцию, возвращающую `Flow<ResponseProto>`:
```kotlin
// commonMain: dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
interface RecipeRemoteDataSource {
    fun streamRecommendations(request: RecommendationFilter): Flow<Recipe>
}

// commonMain: data/remote/grpc/RecipeGrpcRemoteDataSource.kt
class RecipeGrpcRemoteDataSource(
    private val client: RecipeGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
) : RecipeRemoteDataSource {

    override fun streamRecommendations(request: RecommendationFilter): Flow<Recipe> {
        return client.streamRecommendations(request.toProto())
            .map { proto -> proto.toDomain() }
            .flowOn(dispatchers.io)
            // При отмене сбора flow (cancellation) grpc-kotlin автоматически отправляет RST_STREAM
    }
}
```

### 4.3 Безопасная неблокирующая авторизация (Ликвидация runBlocking)
Для Android используется класс `AuthCallCredentials` (наследующий `io.grpc.CallCredentials`):
- Перехватывает вызов метаданных перед отправкой по сети;
- Асинхронно обновляет токен в `dispatchers.io` через `tokenProvider.refreshIfNeeded()`;
- Прикрепляет заголовок `authorization: Bearer <token>`;
- Применяет метаданные через `appExecutor.execute { applier.apply(metadata) }` без единого вызова `runBlocking`.

### 4.4 Заметка по реализации для iOS
При подключении таргета `iosArm64` / `iosSimulatorArm64`:
1. В `iosMain` создается реализация `IosProfileRemoteDataSource : ProfileRemoteDataSource`;
2. Для взаимодействия с сетью подключается официальный Swift-пакет `grpc-swift` от Apple через Swift Package Manager или Cocoapods;
3. Для авторизации токен извлекается из **iOS Keychain**;
4. Экземпляр регистрируется в DI-контейнере Koin через `iosModule` (`single<ProfileRemoteDataSource> { ... }`).
Доменный слой, репозитории (`ProfileRepositoryImpl`), MVI-хранилища KoTEA (`ProfileStore`) и экраны остаются на 100% переиспользуемыми без правок.

---

## 5. Статус реализации в проекте FitAssistent2

- [x] **Интерфейс RemoteDataSource в `commonMain`**: [`ProfileRemoteDataSource.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/commonMain/kotlin/dev/andre/nutritionist/data/remote/ProfileRemoteDataSource.kt);
- [x] **Unary gRPC клиент в `commonMain`**: [`ProfileGrpcRemoteDataSource.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/commonMain/kotlin/dev/andre/nutritionist/data/remote/grpc/ProfileGrpcRemoteDataSource.kt);
- [x] **Mapper DTO $\leftrightarrow$ Domain**: [`ProfileDtoMapper.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/commonMain/kotlin/dev/andre/nutritionist/data/mapper/ProfileDtoMapper.kt) без оператора `!!`;
- [x] **Фабрика каналов и интерцепторы трассировки**: [`GrpcClientFactory.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidMain/kotlin/dev/andre/nutritionist/data/remote/grpc/GrpcClientFactory.kt), [`TraceparentClientInterceptor.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidMain/kotlin/dev/andre/nutritionist/data/remote/grpc/interceptor/TraceparentClientInterceptor.kt);
- [x] **Неблокирующая авторизация CallCredentials**: [`AuthCallCredentials.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidMain/kotlin/dev/andre/nutritionist/data/remote/grpc/auth/AuthCallCredentials.kt);
- [x] **Koin DI связывание**: [`AndroidDataModule.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidMain/kotlin/dev/andre/nutritionist/di/AndroidDataModule.kt);
- [x] **100% покрытие Unit-тестами без MockK**: [`ProfileGrpcRemoteDataSourceTest.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/commonTest/kotlin/dev/andre/nutritionist/data/remote/grpc/ProfileGrpcRemoteDataSourceTest.kt), [`AuthCallCredentialsTest.kt`](file:///C:/Users/andre/AndroidStudioProjects/FitAssistent2/mobile/shared/src/androidHostTest/kotlin/dev/andre/nutritionist/data/remote/grpc/auth/AuthCallCredentialsTest.kt).
