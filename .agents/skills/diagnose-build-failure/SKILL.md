---
name: diagnose-build-failure
description: >
  Диагностика и минимальное исправление сбоев сборки. Используй этот Skill при падении
  Gradle (компиляция, KSP, Room schema export), Buf (lint, breaking, generate), ktlint,
  detekt, или при ошибках в libs.versions.toml. Запрещает удаление тестов, отключение
  линтеров и добавление @Suppress без обоснования. Всегда ищет корневую причину перед
  применением исправления.
---

# Skill: Diagnose Build Failure

## Назначение

Этот Skill регулирует процесс диагностики и минимального исправления сбоёв сборки.
**Главный принцип:** понять корневую причину, не симптом. Никогда не «глушить» ошибки.

---

## Основные запреты

- **ЗАПРЕЩЕНО** отключать линтеры (`ktlint { disabledRules = ... }`, `detekt { ... }.enabled = false`).
- **ЗАПРЕЩЕНО** удалять тесты для прохождения CI.
- **ЗАПРЕЩЕНО** добавлять `@Suppress("...")` без комментария с обоснованием.
- **ЗАПРЕЩЕНО** добавлять `build.gradle.kts: suppressWarnings = true`.
- **ЗАПРЕЩЕНО** игнорировать `buf breaking` — только версионирование или откат.
- **ЗАПРЕЩЕНО** хардкодить версии в `build.gradle.kts` для «быстрого фикса».

---

## Workflow

### Шаг 1. Прочитать STATE.md и сообщение об ошибке

```
1. Прочитать STATE.md — понять контекст (что менялось).
2. Прочитать полный stack trace / сообщение об ошибке.
3. Определить тип сбоя: Gradle компиляция / KSP / Buf / ktlint / detekt / версии.
```

### Шаг 2. Диагностика по типу ошибки

---

#### Тип A. Ошибка компиляции Kotlin

**Симптом:** `error: unresolved reference`, `type mismatch`, `overload resolution ambiguity`

**Диагностика:**
```bash
# 1. Запустить с полным стектрейсом
./gradlew :app:shared:compileKotlinAndroid --stacktrace --info 2>&1 | head -100

# 2. Проверить, не появились ли новые нарушения AGENTS.md:
grep -rn "!!" app/shared/src/commonMain/
grep -rn "import java\." app/shared/src/commonMain/
grep -rn "import android\." app/shared/src/commonMain/
```

**Типичные причины и исправления:**

| Ошибка | Причина | Исправление |
|---|---|---|
| `Unresolved reference: Uuid` | Нет `kotlin.uuid.Uuid` в Kotlin < 2.0 | Обновить Kotlin до 2.0+ в `libs.versions.toml` |
| `Cannot access 'java.time.Instant'` | `java.time.*` в `commonMain` | Заменить на `kotlinx.datetime.Instant` |
| `Type mismatch: String vs RecipeId` | Сырой String вместо value class | Обернуть: `RecipeId(rawString)` |
| `Overload resolution ambiguity` | Конфликт версий зависимостей | Проверить `./gradlew :app:shared:dependencies` |

---

#### Тип B. KSP / Room KMP — ошибки схемы

**Симптом:** `Room schema export directory is not set`, `Cannot find symbol @Entity`, `Duplicate class`

**Диагностика:**
```bash
# Запустить KSP отдельно
./gradlew :app:shared:kspCommonMainKotlinMetadata --stacktrace
./gradlew :app:shared:kspAndroidDebugKotlin --stacktrace
```

**Типичные причины:**

| Ошибка | Причина | Исправление |
|---|---|---|
| `Schema export directory is not set` | Нет `room { schemaDirectory(...) }` | Добавить в `build.gradle.kts` |
| `Multiple KSP processors` | Дублирование KSP зависимости | Проверить `add("ksp...")` блоки |
| `Cannot find symbol @Entity` | Room аннотации не в classpath для таргета | Добавить `add("kspIosArm64", libs.room.compiler)` |
| `Incompatible schema version` | Изменена Entity без создания Migration | Добавить `AutoMigration` или ручную Migration |

**Правильная конфигурация KSP для Room KMP:**
```kotlin
// build.gradle.kts (:app:shared)
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)     // если есть jvmMain
}
```

---

#### Тип C. Ошибки `libs.versions.toml` / Gradle

**Симптом:** `Could not find libs.xxx`, `Version conflict`, `TOML parse error`

**Диагностика:**
```bash
# Проверить синтаксис TOML
./gradlew :app:shared:dependencies --configuration commonMainImplementation

# Посмотреть resolved versions
./gradlew dependencyInsight --dependency kotlinx-coroutines-core --configuration commonMainImplementation
```

**Правила исправления:**
1. Все версии — **только** в `[versions]` секции.
2. Алиасы — в `[libraries]` через `version.ref = "..."`.
3. Нет дублирования версий (одна `kotlinx-coroutines` версия для всех модулей).

**Пример — правильно:**
```toml
[versions]
kotlinx-coroutines = "1.10.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

**Пример — запрещено:**
```kotlin
// build.gradle.kts — ЗАПРЕЩЕНО:
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
```

---

#### Тип D. Buf lint / breaking

**Симптом:** `buf lint: FIELD_NO_DELETE`, `SERVICE_NO_DELETE`, `COMMENT_FIELD`

**Диагностика:**
```bash
# Запустить с детальным выводом
buf lint proto/ --format json 2>&1 | python3 -m json.tool

# Проверить breaking changes
buf breaking proto/ --against '.git#branch=main' --format json
```

**Исправление lint ошибок:**

| Ошибка | Исправление |
|---|---|
| `COMMENT_FIELD` | Добавить doc-комментарий к полю |
| `COMMENT_MESSAGE` | Добавить doc-комментарий к message |
| `COMMENT_RPC` | Добавить doc-комментарий к RPC |
| `ENUM_VALUE_PREFIX` | Добавить префикс: `STATUS_UNSPECIFIED` вместо `UNSPECIFIED` |
| `ENUM_ZERO_VALUE_SUFFIX` | Нулевое значение должно заканчиваться на `_UNSPECIFIED` |
| `PACKAGE_VERSION_SUFFIX` | Добавить `.v1` в имя пакета |

**Исправление breaking changes:**
- Если breaking change **случайный** (опечатка, лишнее удаление) → откатить изменение в .proto.
- Если breaking change **необходим** → создать `v2` версию пакета, поддерживать `v1` параллельно.
- **НИКОГДА** не игнорировать `buf breaking` через `--ignore`.

---

#### Тип E. ktlint / detekt

**Симптом:** `Exceeded max line length`, `Wildcard import`, `UnusedImport`, `FunctionNaming`

**Диагностика:**
```bash
# ktlint — детальный вывод
./gradlew ktlintCheck --continue 2>&1 | grep "error\|warning" | head -50

# detekt — по файлу
./gradlew detekt --input app/shared/src/commonMain/ 2>&1
```

**Исправление:**
```bash
# ktlint — автоисправление (безопасно для форматирования)
./gradlew ktlintFormat

# После ktlintFormat — проверить, что логика не изменилась
git diff app/shared/
```

**detekt — исправление вручную:**
- `ComplexMethod`: разбить на несколько функций.
- `LongMethod`: вынести блоки в отдельные private fun.
- `MagicNumber`: вынести в const val.
- `UnusedPrivateMember`: удалить неиспользуемый код.
- `ForbiddenImport` (java.*/android.* в commonMain): заменить на KMP-аналоги.

**Если detekt ошибочно срабатывает (false positive):**
```kotlin
// Допустимо ТОЛЬКО с обоснованием:
@Suppress("MagicNumber") // обоснование: константа соответствует стандарту RFC-7234 §5.3 (max-age)
private const val MAX_AGE_SECONDS = 3600
```

---

#### Тип F. Тесты падают

**Симптом:** `AssertionError`, `TestTimedOutException`, `Container startup failed`

**Диагностика:**
```bash
# Запустить конкретный тест с подробным выводом
./gradlew :app:shared:allTests --tests "*.GetRecipeUseCaseTest" --info

# Backend — с Testcontainers логами
./gradlew :server:test --tests "*.DiaryServiceIntegrationTest" --info \
  -DTESTCONTAINERS_RYUK_DISABLED=true
```

**Правила исправления тестов:**
- Никакого `Thread.sleep()` → использовать `advanceTimeBy()` / `runTest`.
- Никакого моков Docker-контейнеров → поднять реальный Docker.
- `cancelAndIgnoreRemainingEvents()` обязателен в Turbine тестах.
- Если тест нестабилен (flaky) → проверить `TestDispatcher` тип (`Standard` vs `Unconfined`).

---

### Шаг 3. Применить минимальное исправление

**Принцип минимальности:**
- Исправлять только то, что сломано.
- Не рефакторить соседний код «пока рядом».
- Не менять API, если не требуется.

### Шаг 4. Верифицировать исправление

```bash
# 1. Проверить исправленный модуль
./gradlew :<module>:build

# 2. Убедиться, что соседние модули не сломались
./gradlew build --continue

# 3. Тесты
./gradlew :app:shared:allTests :server:test

# 4. Линтеры
./gradlew ktlintCheck detekt buf lint proto/
```

### Шаг 5. Обновить STATE.md

Записать:
- Что было сломано и причина.
- Как было исправлено.
- Если нашли системную проблему — записать в «Известные проблемы».

---

## Диагностическая карта

```
СБОЙ СБОРКИ
    │
    ├─→ Kotlin compile error?
    │       └─→ Проверить AGENTS.md нарушения (!!,  java.*, android.*)
    │           └─→ Проверить версии зависимостей (libs.versions.toml)
    │
    ├─→ KSP / Room error?
    │       └─→ Проверить schemaDirectory в build.gradle.kts
    │           └─→ Проверить add("ksp<Target>", libs.room.compiler) для каждого таргета
    │               └─→ Проверить incrementVersion + Migration
    │
    ├─→ libs.versions.toml error?
    │       └─→ Проверить синтаксис TOML
    │           └─→ Проверить дублирование версий
    │               └─→ Запустить ./gradlew dependencies
    │
    ├─→ buf lint/breaking?
    │       └─→ buf lint proto/ --format json
    │           └─→ Добавить комментарии / переименовать / создать v2
    │
    ├─→ ktlint?
    │       └─→ ./gradlew ktlintFormat (безопасно)
    │           └─→ Проверить git diff
    │
    ├─→ detekt?
    │       └─→ Исправить вручную (разбить методы, удалить мёртвый код)
    │           └─→ @Suppress только с обоснованием
    │
    └─→ Tests fail?
            └─→ Изолировать: запустить один тест
                └─→ Проверить TestDispatcher
                    └─→ Проверить cancelAndIgnoreRemainingEvents()
                        └─→ Проверить Docker запущен (Testcontainers)
```
