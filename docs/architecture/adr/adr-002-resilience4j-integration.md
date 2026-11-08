# ADR-002: Выбор стратегии интеграции Resilience4j в экосистему Spring Boot 4.1 с Kotlin Coroutines

* **Статус**: Accepted (Принято)
* **Дата**: 2026-11-08
* **Контекст задачи**: [T-013] Spike / KAN-13 (Интеграция Resilience4j в backend-сервисы)
* **Авторы**: `implementer` (архитектура и спайк), `test-reviewer` (аудит надежности и корутин)

---

## Контекст и проблематика (Context)

В проекте «Бюджетный нутрициолог» (FitAssistent2) бэкенд построен на базе **Spring Boot 4.1.x**, **Kotlin 2.4.x**, **gRPC Kotlin Stubs** (`grpc-kotlin`) и асинхронных корутин (`suspend fun`).
Бэкенд-сервисы взаимодействуют как между собой по gRPC, так и с внешними ненадежными ресурсами:
1. `recipe-ai-service`: обращение к внешним LLM API (генерация бюджетных планов питания) с задержками до 5–15 секунд и риском rate-limit;
2. `food-cv-service`: обращение к тяжелым ML/CV моделям распознавания блюд по фото;
3. `catalog-price-service`: интеграция с внешними парсерами цен супермаркетов и каталогами продуктов;
4. Межсервисный gRPC-обмен (например, `diary-service` $\to$ `catalog-price-service`).

Для предотвращения каскадных сбоев (Cascading Failures) и исчерпания ресурсов пулов соединений необходимы паттерны отказоустойчивости: **Circuit Breaker**, **Retry** (для идемпотентных операций) и **Rate Limiter**.

### Специфика Kotlin Coroutines и Spring Boot 4.1:
Классический подход Resilience4j в Spring построен на Java AOP-прокси (`@Aspect`, `@CircuitBreaker`, `@Retry`). В среде Kotlin Coroutines методы компилируются в байткод с дополнительным аргументом `Continuation<T>` и возвращают `COROUTINE_SUSPENDED`. Это порождает фундаментальные технические проблемы:
- **Преждевременное завершение перехвата (Early AOP Exit):** AOP-перехватчик видит маркер `COROUTINE_SUSPENDED` как нормальный результат выполнения и фиксирует успешный вызов до того, как корутина фактически возобновилась и завершилась;
- **Потеря исключений и сбоев:** исключения, выброшенные внутри возобновленной корутины, не попадают в синхронный `catch` AOP-аспекта;
- **Ложное срабатывание на отмену (`CancellationException`):** при отмене клиентского gRPC-запроса в корутине выбрасывается `CancellationException`. Некорректный перехватчик считает это сетевой ошибкой и открывает Circuit Breaker;
- **Опасность Thread Starvation:** попытки адаптации через `runBlocking` блокируют потоки диспетчеров корутин или Netty event loops;
- **Повторное использование `Continuation` в `@Retry`:** вызов `proceed()` повторно на том же `Continuation` приводит к `IllegalStateException: Already resumed`.

---

## Факторы принятия решений (Decision Drivers)

1. **Безупречная совместимость с корутинами (`suspend fun`):** отсутствие блокировок потоков (`Thread Starvation`), отсутствие `runBlocking`, корректное измерение задержки от старта до реального возобновления/завершения;
2. **Корректная обработка структурированной конкурентности (Cancellation Safety):** `CancellationException` никогда не должен учитываться как ошибка удаленного сервиса и должен беспрепятственно пробрасываться дальше;
3. **Чистота архитектуры (Clean Architecture Isolation):** бизнес-логика (domain/usecase) не должна зависеть от аннотаций фреймворка Resilience4j; отказоустойчивость должна накладываться на уровне инфраструктурных шлюзов/адаптеров;
4. **Минимальный оверхед в рантайме (Zero/Low Runtime Overhead):** отсутствие громоздких адаптеров между Reactive Streams и корутинами;
5. **Совместимость с версиями Spring Boot 4.1 и Gradle 9.x:** отсутствие циклических зависимостей и конфликтов транзитивных библиотек.

---

## Рассмотренные варианты (Considered Options)

1. **Вариант 1: Официальный стартер (`resilience4j-spring-boot3` / AOP-аннотации `@CircuitBreaker`)**;
2. **Вариант 2: Чистое ядро (`resilience4j-circuitbreaker`, `resilience4j-retry`) + `resilience4j-kotlin` (Функциональные расширения для корутин)**;
3. **Вариант 3: Абстракция Spring Cloud CircuitBreaker (`spring-cloud-starter-circuitbreaker-reactor-resilience4j`)**.

---

## Сравнительный анализ вариантов (Pros and Cons)

### Вариант 1: Официальный стартер (AOP + `@CircuitBreaker`)

* **Суть**: Подключение стартера, конфигурирование в `application.yml`, навешивание `@CircuitBreaker(name = "...", fallbackMethod = "...")` на методы бинов.
* **Плюсы**:
  * Знакомый декларативный синтаксис аннотаций;
  * Автоматическая интеграция со Spring Boot Actuator `/actuator/health` и метриками Micrometer.
* **Минусы**:
  * ❌ **Хрупкость с `suspend fun`**: в версиях Resilience4j поддержка корутин в AOP-аспектах является вторичной надстройкой, подверженной сбоям при использовании кастомных диспетчеров;
  * ❌ **Ломает `@Retry`**: повторный вызов `joinPoint.proceed()` в корутинах приводит к падению из-за однократности `Continuation`;
  * ❌ **Нарушение чистоты домена (§2.2 `AGENTS.md`)**: разработчики стремятся вешать аннотации прямо на доменные сервисы или UseCase, привязывая чистую архитектуру к Spring AOP;
  * ❌ **Сложность тестирования**: невозможно протестировать логику отката в чистом unit-тесте без поднятия Spring AOP контекста.

### Вариант 2: Чистое ядро + `resilience4j-kotlin` (Функциональные расширения)

* **Суть**: Включение базовых библиотек `resilience4j-circuitbreaker`, `resilience4j-retry` и `resilience4j-kotlin`. Создание Spring `@Configuration` бинов реестров (`CircuitBreakerRegistry`), конфигурируемых из `application.yml`. Инфраструктурные клиенты оборачивают реальные вызовы с помощью DSL-расширений: `circuitBreaker.executeSuspend { ... }` или `withCircuitBreaker(cb) { ... }`.
* **Плюсы**:
  * ✅ **100% нативно для корутин**: прямое выполнение внутри вызывающего `CoroutineContext` без магии AOP и рефлексии;
  * ✅ **Гарантия Cancellation Safety**: явная проверка и проброс `CancellationException` без фиксации ложного сбоя;
  * ✅ **Изоляция Clean Architecture**: Domain и Usecase остаются полностью независимыми от внешних фреймворков. Обёртки применяются строго в инфраструктурных клиентах (`*RemoteDataSource`, `*ClientAdapter`);
  * ✅ **Нулевой оверхед на потоки**: нет промежуточных пулов, нет блокировок, нет `runBlocking`;
  * ✅ **Простота unit-тестирования**: клиент тестируется обычным unit-тестом, в который передается настроенный `CircuitBreaker.ofDefaults("test")` без Spring Test context;
  * ✅ **Полная поддержка метрик**: `CircuitBreakerRegistry` легко регистрируется в `MeterRegistry` (Micrometer/Actuator).
* **Минусы**:
  * ⚠️ Требуется явное создание DSL-оберток/extension-функций в инфраструктурном слое (но это компенсируется типобезопасностью и прозрачностью кода).

### Вариант 3: Spring Cloud CircuitBreaker (Reactive Streams abstraction)

* **Суть**: Использование фабрики `ReactiveCircuitBreakerFactory`, оборачивание вызовов в `Mono<T>` с последующей конвертацией обратно в корутины через `mono.awaitSingle()`.
* **Плюсы**:
  * Унифицированная абстракция от Spring Cloud.
* **Минусы**:
  * ❌ **Двойная адаптация**: `suspend` $\to$ `Mono` $\to$ Reactor CircuitBreaker Operator $\to$ `Mono` $\to$ `suspend` приводит к ненужному созданию промежуточных объектов и накладным расходам;
  * ❌ **Тяжелые зависимости**: тянет за собой весь `spring-cloud-commons` и `reactor-core`, усложняя Version Catalog (`libs.versions.toml`);
  * ❌ **Рассинхронизация версий**: релизные циклы Spring Cloud часто отстают от мажорных обновлений Spring Boot (особенно линейки 4.x);
  * ❌ **Потеря контекста трассировки**: проброс W3C `traceparent` и MDC через границу Kotlin Coroutines $\leftrightarrow$ Project Reactor требует громоздких мостов (`ReactorContext`).

---

## Сводная таблица сравнения

| Критерий | 1. Стартер (AOP аннотации) | 2. Ядро + `resilience4j-kotlin` | 3. Spring Cloud CircuitBreaker |
| :--- | :--- | :--- | :--- |
| **Нативность для Kotlin Coroutines** | ⚠️ Низкая (AOP хаки с Continuation) | 🟢 **Идеальная** (чистый `suspend`) | ⚠️ Опосредованная (через Mono) |
| **Риск Thread Starvation** | ⚠️ Средний (при ошибочном TimeLimiter) | 🟢 **Отсутствует** (non-blocking) | 🟢 Отсутствует |
| **CancellationException Safety** | ❌ Риск ложного trip breaker | 🟢 **Гарантированная защита** | ⚠️ Зависит от Reactive bridge |
| **Изоляция Domain-слоя (Clean Arch)** | ❌ Провоцирует аннотации в domain | 🟢 **Полная изоляция в клиентах** | 🟢 Изоляция в клиентах |
| **Накладные расходы (Overhead)** | ⚠️ AOP рефлексия, proxy | 🟢 **Минимальные (inline functions)** | ❌ Высокие (Mono + Reactor Context) |
| **Совместимость со Spring Boot 4.1** | ⚠️ Зависит от AOP стартера | 🟢 **Абсолютная (нет привязки к AOP)**| ❌ Риск конфликтов Spring Cloud BOM |
| **Удобство Unit-тестирования** | ❌ Требует Spring / AOP mock | 🟢 **Тривиальное (Fake CB Registry)** | ⚠️ Требует Reactor StepVerifier |

---

## Решение (Decision Outcome)

Принять **Вариант 2: Чистое ядро Resilience4j (`resilience4j-circuitbreaker`, `resilience4j-retry`, `resilience4j-micrometer`) + функциональные расширения Kotlin (`resilience4j-kotlin`)**.

### Архитектурные принципы внедрения:
1. **Регистрация в DI**: Конфигурировать `CircuitBreakerRegistry` и `RetryRegistry` как Spring `@Bean`, считывая параметры из `application.yml` через `@ConfigurationProperties`. Регистрировать реестры в `MeterRegistry` для экспорта метрик в Actuator/Prometheus;
2. **Инфраструктурная изоляция (§2.2 `AGENTS.md`)**: Запретить аннотации и вызовы Resilience4j в пакетах `domain/` и `service/`. Все механизмы защиты размещаются строго в `infrastructure/client/` или `data/remote/`;
3. **Безопасный корутинный вызов**: Использовать канонический паттерн `executeSuspend` с обязательным перехватом и немедленным пробросом `CancellationException`;
4. **Идемпотентный Retry (§4.5 `AGENTS.md`)**: Применять повторные попытки (`Retry`) **исключительно** к идемпотентным операциям (GET, чтение каталога, либо запросы с обязательным заголовком `idempotency-key`). Мутирующие операции без ключа идемпотентности повторять **запрещено**;
5. **Fallback-стратегия**: Fallback-методы для внешних сервисов (LLM, парсеры цен) должны возвращать деградированные данные из кэша Redis или статические значения, никогда не выбрасывая непроверенных исключений.

---

## Последствия (Consequences)

### Положительные:
- Полное отсутствие блокировок потоков (`Thread Starvation`) и прозрачное выполнение в диспетчерах `dispatchers.io`;
- Корректная работа клиентских тайм-аутов и отмен в gRPC без ложного перехода Circuit Breaker в состояние `OPEN`;
- Высокая скорость сборки и отсутствие тяжелых зависимостей Spring Cloud;
- 100% покрытие инфраструктурных клиентов быстрыми unit-тестами с `Fake` репозиториями без тяжелых контекстов Spring.

### Отрицательные / Риски и их минимизация:
- **Риск:** Разработчик может забыть обработать `CancellationException` при ручной реализации декоратора.
  - *Минимизация:* Предоставить единый базовый модуль/класс-утилиту `ResilienceExtensions.kt` с готовыми безопасными функциями `executeWithCircuitBreaker` и `executeWithRetry`.
- **Риск:** Конфигурация в `application.yml` требует явного биндинга в properties-класс.
  - *Минимизация:* Реализовать типобезопасный `ResilienceProperties` бин со стандартными дефолтными профилями.
