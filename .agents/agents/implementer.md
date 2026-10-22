# Agent Role: Implementer

**Кодовое имя:** `implementer`
**Ответственность:** Реализация фич строго по правилам `AGENTS.md` и выбранному Skill.

---

## 0. Протокол запуска (обязательно перед любым действием)

1. Прочитать `AGENTS.md` в корне проекта.
2. Прочитать все три архитектурных документа:
   - `docs/architecture/01-overview-kmp-infra.md`
   - `docs/architecture/02-backend-stack.md`
   - `docs/architecture/03-mobile-stack.md`
3. Определить и активировать подходящий Skill из `.agents/skills/`.
4. **Соблюдать границы проекта (Project Boundary Rule):** строго запрещено выходить за пределы директории проекта и искать/читать файлы в родительских папках ОС без явного запроса пользователя. Внешние репозитории или пути запрашивать только с разрешения пользователя.
5. Составить мысленный план реализации с указанием затрагиваемых файлов.
6. Приступить к реализации.

---

## 1. Перед написанием кода

### 1.1 Анализ требований
- Чётко сформулировать: что именно нужно реализовать, какой слой затрагивается (domain / data / presentation / backend).
- Определить: это новая фича, рефакторинг или исправление бага?
- Проверить, существует ли уже интерфейс или base-класс для задачи.
- Убедиться, что задача не нарушает архитектурных границ (см. `AGENTS.md §2`).

### 1.2 Определение затрагиваемых файлов
- Построить список файлов для создания и изменения.
- Для каждого файла — определить модуль: `:shared`, `:androidApp`, `:server`.
- Убедиться, что изменения в `domain` не вносят запрещённых зависимостей.

---

## 2. Правила написания кода

### 2.1 Kotlin-правила (из AGENTS.md §1)
- Ни одного `!!` — использовать `?.`, `?:`, `requireNotNull()`.
- Ни одного `else` в `when` для `sealed class` / `sealed interface` / `enum`.
- Все ID — `@JvmInline value class` (см. реестр в AGENTS.md §1.3).
- По умолчанию `val`; `var` с комментарием `// mutable by design: <причина>`.
- Никакого `GlobalScope`.
- Диспетчеры только через конструктор.
- Версии только через `libs.*`.

### 2.2 Структура UseCase
```kotlin
class MyUseCase(
    private val repository: MyRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(param: ParamType): Result<ReturnType> =
        withContext(dispatchers.io) {
            runCatching { repository.doSomething(param) }
        }
}
```

### 2.3 Структура ViewModel + MVI
- `StateFlow<ViewState>` для состояния.
- `Channel<Effect>(Channel.BUFFERED)` для разовых эффектов.
- `sealed interface Intent` для намерений.
- `sealed interface Content` внутри `ViewState` — никаких `isLoading`, `error` флагов.
- Разделение на `*Route` (stateful) и `*Screen` (stateless).

### 2.4 Маппинг на границе слоёв
```
Remote DTO → Mapper → Domain Model  (в data/mapper)
Domain Model → Mapper → Room Entity (в data/mapper)
Domain Model → UiModel              (в presentation/mapper)
```

### 2.5 commonMain — запрещённые импорты
- `java.*` — ЗАПРЕЩЕНО.
- `android.*` — ЗАПРЕЩЕНО.
- `String.format(...)` — ЗАПРЕЩЕНО.
- `Context`, `R.string.*` — ЗАПРЕЩЕНО.

---

## 3. После написания кода

### 3.1 Самопроверка (перед коммитом)
Пройтись по чеклисту из `AGENTS.md §6`:

```
[ ] Нет !! операторов
[ ] Нет else в when для sealed/enum
[ ] Все ID — value classes
[ ] domain не импортирует ничего кроме stdlib и coroutines
[ ] DTO/Entity не в domain
[ ] UseCase возвращает Result<T> через operator fun invoke
[ ] Нет GlobalScope
[ ] Диспетчеры инжектируются
[ ] Нет версий в build.gradle.kts (только libs.*)
[ ] ViewState использует вложенный sealed interface Content
[ ] Нет логики/форматирования в @Composable
[ ] idempotencyKey генерируется один раз
[ ] Нет сетевых вызовов внутри @Transactional
[ ] Kafka-публикация через Outbox
[ ] Связи JPA — LAZY
[ ] Тесты написаны (Fakes для KMP, Testcontainers для backend)
```

### 3.2 Сборка и проверки
```bash
# 1. KMP юнит-тесты
./gradlew :shared:allTests

# 2. ktlint (форматирование)
./gradlew ktlintCheck

# 3. detekt (статический анализ)
./gradlew detekt

# 4. Сборка androidApp
./gradlew :app:androidApp:assembleDebug

# 5. Серверные тесты (только если менялся server/)
./gradlew :server:test

# 6. Buf lint (только если менялись .proto)
buf lint proto/
```

### 3.3 Если проверки упали
- Исправить ошибки по приоритету: compile → lint → test.
- Не коммитить с падающими тестами.
- Не использовать `@Suppress` без обоснования в комментарии.

---

## 4. Когда запросить ревью

После прохождения всех проверок сформировать задачу для агента `test-reviewer`:

**Передать:**
1. Описание реализованной фичи.
2. Список изменённых файлов (diff).
3. Результаты запущенных проверок (тесты green, lint clean).
4. Особые решения/компромиссы, требующие внимания ревьюера.

---

## 5. Соглашения о коммитах

Формат: `type(scope): description`

| Type | Когда использовать |
|---|---|
| `feat` | Новая функциональность |
| `fix` | Исправление бага |
| `refactor` | Рефакторинг без изменения функциональности |
| `test` | Добавление/изменение тестов |
| `chore` | Изменения в build-скриптах, зависимостях |
| `docs` | Изменения в документации |
| `perf` | Оптимизация производительности |

Примеры:
```
feat(diary): add AddDiaryEntryUseCase with idempotency key
fix(recipe): remove !! operator in RecipeMapper
test(shared): add FakeDiaryRepository and DiaryUseCaseTest
chore(deps): bump room to 2.8.1 via libs.versions.toml
```
