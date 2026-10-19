---
name: kmp-shared-feature
description: >
  Разработка бизнес-логики в :shared (KMP). Используй этот Skill при создании или изменении
  domain/model с @JvmInline value class, domain/repository (интерфейсы), domain/usecase
  (operator fun invoke + Result<T>), data/local на Room KMP 2.8.x, data/remote на gRPC,
  data/mapper, di на Koin. Обязателен при любой работе в commonMain, androidMain, iosMain.
  Запрещает java.*/android.*/Context/R.string/!! в commonMain.
---

# Skill: KMP Shared Feature

## Назначение

Этот Skill регулирует разработку любой бизнес-логики, которая живёт в модуле `:shared`.
Он охватывает всю цепочку от domain-модели до реализации репозитория и DI-модуля.

---

## Workflow

### Шаг 1. Прочитать STATE.md и архитектурный документ

```
1. Прочитать STATE.md — понять текущий статус.
2. Прочитать docs/architecture/01-overview-kmp-infra.md — структура :shared, Koin, Room KMP, gRPC.
3. Определить: что создаётся (новая фича) или изменяется (рефакторинг)?
```

### Шаг 2. Domain Layer — создать модели и интерфейсы

#### 2.1 Добавить Value Class в `ids.kt`
```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/model/ids.kt
@JvmInline value class NewEntityId(val raw: String)
```
- Никаких сырых `String` / `Int` / `Long` как ID.
- Добавить в реестр в `AGENTS.md §1.3`.

#### 2.2 Создать Domain Model
```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/model/NewEntity.kt
package com.budgetntr.shared.domain.model

// Только чистый Kotlin + kotlinx-datetime + kotlin.uuid
data class NewEntity(
    val id: NewEntityId,
    val name: String,
    val createdAt: Instant,       // kotlinx-datetime, НЕ java.time
)
```
**Запрещены в этом файле:** `import java.*`, `import android.*`, `import androidx.*`.

#### 2.3 Создать Repository Interface
```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/repository/NewEntityRepository.kt
package com.budgetntr.shared.domain.repository

interface NewEntityRepository {
    suspend fun getById(id: NewEntityId): NewEntity
    suspend fun save(entity: NewEntity)
    fun observeAll(): Flow<List<NewEntity>>
}
```

#### 2.4 Создать UseCase
```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/domain/usecase/newentity/GetNewEntityUseCase.kt
package com.budgetntr.shared.domain.usecase.newentity

class GetNewEntityUseCase(
    private val repository: NewEntityRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(id: NewEntityId): Result<NewEntity> =
        withContext(dispatchers.io) {
            runCatching { repository.getById(id) }
        }
}
```
- `operator fun invoke` — обязательно.
- `Result<T>` — обязательно (Fail Fast через `runCatching`).
- Диспетчеры через конструктор — обязательно.

### Шаг 3. Data Layer — Room Entity + DAO

#### 3.1 Room Entity
```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/local/entity/NewEntityEntity.kt
@Entity(tableName = "new_entities")
data class NewEntityEntity(
    @PrimaryKey val id: String,          // String в Entity, NewEntityId в domain
    val name: String,
    val createdAtMs: Long,               // epochMillis → Instant в domain через mapper
    val syncStatus: String = "PENDING",
)
```

#### 3.2 Room DAO
```kotlin
@Dao
interface NewEntityDao {
    @Query("SELECT * FROM new_entities WHERE id = :id")
    suspend fun getById(id: String): NewEntityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: NewEntityEntity)

    @Query("SELECT * FROM new_entities")
    fun observeAll(): Flow<List<NewEntityEntity>>

    @Query("SELECT * FROM new_entities WHERE syncStatus = 'PENDING'")
    fun observePending(): Flow<List<NewEntityEntity>>
}
```

#### 3.3 Добавить DAO в AppDatabase
```kotlin
@Database(
    entities = [/* существующие */, NewEntityEntity::class],
    version = N, // increment
    exportSchema = true,   // ОБЯЗАТЕЛЬНО — schemas/ для Room KMP
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun newEntityDao(): NewEntityDao
}
```
- `exportSchema = true` — всегда.
- `schemaDirectory` в `build.gradle.kts` через `room { schemaDirectory(...) }`.

### Шаг 4. Mapper — граница слоёв

```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/mapper/NewEntityMapper.kt
object NewEntityMapper {
    fun NewEntityEntity.toDomain(): NewEntity = NewEntity(
        id = NewEntityId(id),
        name = name,
        createdAt = Instant.fromEpochMilliseconds(createdAtMs),
    )

    fun NewEntity.toEntity(): NewEntityEntity = NewEntityEntity(
        id = id.raw,
        name = name,
        createdAtMs = createdAt.toEpochMilliseconds(),
    )
}
```
- Mapper — единственное место, где происходит конвертация.
- DTO (Protobuf) → Domain: аналогичный mapper в `data/mapper/proto/`.
- Domain → Entity: аналогичный mapper в `data/mapper/room/`.

### Шаг 5. Repository Implementation

```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/data/repository/NewEntityRepositoryImpl.kt
class NewEntityRepositoryImpl(
    private val dao: NewEntityDao,
    private val remoteDataSource: NewEntityRemoteDataSource,
    private val dispatchers: CoroutineDispatchers,
) : NewEntityRepository {

    override suspend fun getById(id: NewEntityId): NewEntity =
        withContext(dispatchers.io) {
            dao.getById(id.raw)
                ?.toDomain()
                ?: remoteDataSource.fetchById(id).also { entity ->
                    dao.insertOrReplace(entity.toEntity())
                }
        }

    override suspend fun save(entity: NewEntity): Unit =
        withContext(dispatchers.io) {
            dao.insertOrReplace(entity.toEntity())
        }

    override fun observeAll(): Flow<List<NewEntity>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
}
```

### Шаг 6. Koin DI Module

```kotlin
// app/shared/src/commonMain/kotlin/com/budgetntr/shared/di/DataModule.kt
val dataModule = module {
    // добавить к существующим биндингам:
    factory { NewEntityRepositoryImpl(get(), get(), get()) } bind NewEntityRepository::class
}

// app/shared/src/commonMain/kotlin/com/budgetntr/shared/di/DomainModule.kt
val domainModule = module {
    factory { GetNewEntityUseCase(get(), get()) }
}
```

### Шаг 7. Проверка — запрещённые импорты в commonMain

Перед коммитом запустить grep-проверку:
```bash
# Не должно быть результатов:
grep -rn "import java\." app/shared/src/commonMain/
grep -rn "import android\." app/shared/src/commonMain/
grep -rn "String\.format" app/shared/src/commonMain/
grep -rn "import android\.content\.Context" app/shared/src/commonMain/
grep -rn "R\.string\." app/shared/src/commonMain/
grep -rn "!!" app/shared/src/commonMain/
```

### Шаг 8. Тесты (commonTest — только Fakes)

Создать `FakeNewEntityRepository` и тест UseCase:
- См. `.agents/skills/testing-and-verification/SKILL.md` §2.1.
- Никакого MockK/Mockito.
- `BundledSQLiteDriver` для DAO-тестов.

### Шаг 9. Сборка и проверка

```bash
./gradlew :app:shared:allTests           # Unit tests всех таргетов
./gradlew ktlintCheck                     # Форматирование
./gradlew detekt                          # Статический анализ
```

### Шаг 10. Обновить STATE.md

Отметить завершённые файлы, записать следующий шаг.

---

## Чеклист перед коммитом

```
[ ] Нет import java.* в commonMain
[ ] Нет import android.* в commonMain
[ ] Нет !! в commonMain
[ ] Domain model содержит только Kotlin stdlib + kotlinx-datetime + kotlin.uuid
[ ] UseCase: operator fun invoke, Result<T>, runCatching, withContext(dispatchers.io)
[ ] Room Entity: exportSchema = true, schemaDirectory в build.gradle.kts
[ ] Mapper: конвертация только в data/mapper — не в domain, не в presentation
[ ] Koin: фабрики добавлены в соответствующие модули
[ ] Fake-репозиторий создан в commonTest/fake/
[ ] Тест UseCase написан (happy path + failure path)
[ ] ./gradlew :app:shared:allTests — GREEN
```

---

## Ключевые решения и обоснования

| Решение | Обоснование |
|---|---|
| `kotlinx-datetime` вместо `java.time` | iOS-совместимость: `java.time` недоступен в `commonMain` |
| `kotlin.uuid.Uuid` вместо `java.util.UUID` | iOS-совместимость: `java.util.UUID` недоступен в `commonMain` |
| `runCatching` в UseCase | Fail Fast: все исключения превращаются в `Result.Failure` на границе слоя |
| Mapper как object | Нет состояния, нет зависимостей — чистые функции-расширения |
| `exportSchema = true` | Room KMP требует для migration testing и поддержания схемы |
| `flowOn(dispatchers.io)` в Repository | Гарантирует выполнение DB-операций на правильном диспетчере |
