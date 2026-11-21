# 05 — Нагрузка, Кэширование (Redis) и Очереди (Apache Kafka): Расчёт и Формулы

> **Статус документа:** Архитектурный стандарт. Пересмотрен под реальный бюджет и этапность запуска.
>
> **Ключевое ограничение:** Сервер на старте — VPS за **2 000–3 000 ₽/мес** (~4 vCPU, 8 ГБ RAM, 80–120 ГБ NVMe SSD). Весь стек (PostgreSQL + Redis + Kafka + 4 сервиса + Keycloak) живёт на **одной машине** в Docker Compose. Kubernetes и кластеры — Phase 3 (DAU > 5 000).

## 1. Этапность запуска и привязка к бюджету

### Phase 1 — MVP/Бета (Месяцы 1–6)

| Метрика | Значение | Обоснование |
|---|---|---|
| **Бюджет сервера** | 2 000–3 000 ₽/мес | Ограничение пользователя |
| **Что получаем** | 4 vCPU, 8 ГБ RAM, 80–120 ГБ NVMe SSD | Типичный VPS (Timeweb, reg.ru, Selectel) |
| **DAU** | 50–500 | Бета-тест, друзья, первые пользователи |
| **MAU** | 200–2 000 | |
| **Пиковый gRPC RPS** | 1–10 | |
| **Kafka msg/sec** | 0.1–2 | |
| **Топология** | Docker Compose на 1 VPS | Один PostgreSQL, один Redis, один Kafka KRaft |

### Phase 2 — Рост (Месяцы 6–18)

| Метрика | Значение |
|---|---|
| **Бюджет** | 5 000–10 000 ₽/мес |
| **DAU** | 1 000–5 000 |
| **Пиковый RPS** | 10–100 |
| **Топология** | 2 VPS (app+kafka / db+redis), или 1 мощный VPS (8 vCPU, 16 ГБ) |

### Phase 3 — Масштабирование (18+ месяцев)

| Метрика | Значение |
|---|---|
| **Бюджет** | 30 000+ ₽/мес |
| **DAU** | 10 000–50 000 |
| **Пиковый RPS** | 100–1 200 |
| **Топология** | Kubernetes (managed: Yandex Cloud, VK Cloud, Selectel), Redis Cluster 3+3, Kafka 3 брокера |

---

## 2. Формулы расчёта нагрузки (Capacity Planning)

### 2.1 Формула RPS (Requests Per Second)

**Базовая формула среднего RPS:**

$$RPS_{avg} = \frac{DAU \times R_{day}}{T_{active}}$$

Где:
- $DAU$ — количество активных пользователей в день
- $R_{day}$ — количество запросов на одного пользователя в день
- $T_{active}$ — количество «активных» секунд в сутках (не 86 400, а только часы активности)

**Формула пикового RPS:**

$$RPS_{peak} = RPS_{avg} \times k_{peak}$$

Где $k_{peak}$ — коэффициент неравномерности нагрузки:
- Для приложений питания: $k_{peak} = 3\text{–}5$ (80% нагрузки приходится на 3 окна: завтрак/обед/ужин)
- Для стандартных приложений: $k_{peak} = 2\text{–}3$
- Для маркетплейсов (распродажи, чёрная пятница): $k_{peak} = 10\text{–}20$

**Пример для Phase 1 (DAU = 500):**

| Операция | Запросов/день на юзера | Всего/день | Среднее окно (6ч пик) | $RPS_{avg}$ (пик) | $k=3$ → $RPS_{peak}$ |
|---|---|---|---|---|---|
| Чтение профиля | 4 | 2 000 | 21 600 сек | 0.09 | 0.27 |
| Поиск в каталоге | 10 | 5 000 | 21 600 сек | 0.23 | 0.69 |
| Запись дневника | 12 | 6 000 | 21 600 сек | 0.28 | 0.84 |
| AI генерация | 2 | 1 000 | 21 600 сек | 0.05 | 0.15 |
| **Итого** | **28** | **14 000** | | **0.65** | **≈ 2 RPS** |

**Пример для Phase 2 (DAU = 5 000):**

$$RPS_{avg} = \frac{5\,000 \times 28}{21\,600} \approx 6.5 \text{ RPS}$$

$$RPS_{peak} = 6.5 \times 5 = \mathbf{32.5 \text{ RPS}}$$

**Пример для Phase 3 (DAU = 50 000):**

$$RPS_{avg} = \frac{50\,000 \times 28}{21\,600} \approx 65 \text{ RPS}$$

$$RPS_{peak} = 65 \times 7 = \mathbf{455 \text{ RPS}}$$

> [!NOTE]
> С учётом пакетной синхронизации WorkManager (когда устройства выходят из офлайна одновременно) и запаса на будущее, в Phase 3 закладываем потолок **1 200 RPS**.

---

### 2.2 Формула количества партиций Kafka

**Основная формула (Confluent / Jay Kreps):**

$$P = \max\left(\left\lceil \frac{T_{target}}{T_{consumer}} \right\rceil,\ N_{consumers}\right)$$

Где:
- $P$ — число партиций топика
- $T_{target}$ — целевая пропускная способность (msg/sec)
- $T_{consumer}$ — пропускная способность одного потока консьюмера (msg/sec)
- $N_{consumers}$ — число инстансов консьюмера (для параллелизма)

**Пропускная способность одного потока консьюмера:**

$$T_{consumer} = \frac{1}{Latency_{processing}}$$

Если обработка одного сообщения занимает 10 мс:

$$T_{consumer} = \frac{1}{0.010} = 100 \text{ msg/sec}$$

**Примеры расчётов:**

| Phase | $T_{target}$ (msg/sec) | $T_{consumer}$ | $N_{consumers}$ | $P = \max(\lceil T/T_c \rceil, N)$ |
|---|---|---|---|---|
| **Phase 1** (500 DAU) | 0.84 peak | 100 | 1 | $\max(1, 1) = \mathbf{1}$ |
| **Phase 2** (5 000 DAU) | 14 peak | 100 | 2 | $\max(1, 2) = \mathbf{2\text{–}3}$ |
| **Phase 3** (50 000 DAU) | 210 peak | 100 | 6 | $\max(3, 6) = \mathbf{6\text{–}12}$ |

> [!IMPORTANT]
> **Правило:** Партиции можно только добавлять, нельзя убирать. Поэтому на Phase 1 ставим 3 партиции (не 1): это дёшево (1 брокер держит тысячи партиций) и даёт запас для Phase 2 без пересоздания топика.

**Альтернативная формула (от продюсера):**

$$P_{producer} = \left\lceil \frac{T_{target}}{T_{producer\_per\_partition}} \right\rceil$$

На одном брокере KRaft одна партиция обрабатывает ~10 000–50 000 msg/sec при небольших сообщениях (< 1 КБ). При нашем $T_{target} = 0.84$ msg/sec одна партиция загружена на **0.008%** — запас в 12 000 раз.

---

### 2.3 Формула сайзинга оперативной памяти Redis

**Общая формула:**

$$RAM_{total} = \sum_{i=1}^{N} \left( Count_i \times (Key_i + Value_i + Overhead) \right) \times K_{fragmentation}$$

Где:
- $Count_i$ — число ключей категории $i$
- $Key_i$ — средний размер ключа
- $Value_i$ — средний размер значения
- $Overhead$ — Redis internal overhead (~100 байт на ключ: dict entry, robj, SDS, jemalloc alignment)
- $K_{fragmentation}$ — коэффициент фрагментации памяти jemalloc ($1.1\text{–}1.5$, обычно $1.2$)

**Phase 1 (500 DAU):**

| Категория ключей | Формула | $Count$ | $Key$ | $Value$ | $Overhead$ | Итого |
|---|---|---|---|---|---|---|
| Идемпотентность (TTL 24ч) | $DAU \times 12$ записей | 6 000 | 50 Б | 512 Б | 100 Б | **3.8 МБ** |
| Rate Limit бакеты | $DAU \times 2$ бакета | 1 000 | 40 Б | 16 Б | 100 Б | **0.15 МБ** |
| Кэш каталога (TTL 1ч) | Top-1000 товаров | 1 000 | 30 Б | 1 024 Б | 100 Б | **1.1 МБ** |
| Кэш AI рецептов (TTL 12ч) | 100 комбинаций | 100 | 64 Б | 4 096 Б | 100 Б | **0.4 МБ** |
| **Итого без фрагментации** | | | | | | **5.45 МБ** |
| **С фрагментацией (×1.2)** | | | | | | **≈ 6.5 МБ** |

> [!TIP]
> На Phase 1 Redis потребляет **< 10 МБ RAM**. Ему достаточно лимита в 64–128 МБ (`maxmemory 128mb`). Даже при 5 000 DAU (Phase 2) суммарный объём — ~65 МБ. Redis Standalone покрывает это с огромным запасом.

**Phase 3 (50 000 DAU) — расчёт для обоснования перехода на кластер:**

| Категория ключей | $Count$ | Итого (с overhead + frag) |
|---|---|---|
| Идемпотентность | 600 000 | **476 МБ** |
| Rate Limit | 100 000 | **18 МБ** |
| Кэш каталога | 50 000 | **68 МБ** |
| Кэш AI рецептов | 10 000 | **50 МБ** |
| **Итого** | | **≈ 612 МБ** |

При 612 МБ данных Redis Standalone с `maxmemory 1500mb` справляется. **Переход на Redis Cluster оправдан при > 2 ГБ данных или необходимости HA** (SLA 99.9%+), что соответствует DAU > 100 000.

---

### 2.4 Формула дискового пространства Kafka

$$Disk = \frac{T_{avg} \times Msg_{size} \times Retention_{sec} \times RF}{1024^3} \text{ ГБ}$$

Где:
- $T_{avg}$ — средний поток сообщений (msg/sec)
- $Msg_{size}$ — средний размер Avro-сообщения (байт)
- $Retention_{sec}$ — время хранения (секунды)
- $RF$ — Replication Factor

**Phase 1 (500 DAU):**

$$Disk = \frac{0.07 \times 350 \times 604\,800 \times 1}{1024^3} = \frac{14\,817\,600}{1\,073\,741\,824} \approx \mathbf{0.014 \text{ ГБ}} \approx 14 \text{ МБ}$$

**Phase 2 (5 000 DAU):**

$$Disk = \frac{0.7 \times 350 \times 604\,800 \times 1}{1024^3} \approx \mathbf{0.14 \text{ ГБ}} \approx 140 \text{ МБ}$$

**Phase 3 (50 000 DAU, RF=3):**

$$Disk = \frac{7 \times 350 \times 604\,800 \times 3}{1024^3} \approx \mathbf{4.14 \text{ ГБ}}$$

> [!NOTE]
> На Phase 1 Kafka потребляет 14 МБ диска за 7 дней retention. Даже при Phase 2 это 140 МБ. На VPS с 80 ГБ SSD запас > 500x.

---

### 2.5 Формула пропускной способности сети (Network Bandwidth)

$$BW_{peak} = RPS_{peak} \times Msg_{avg\_size}$$

**Phase 1:** $2 \times 4\text{ КБ} = 8 \text{ КБ/сек}$ (0.064 Мбит/сек — ничтожно)

**Phase 2:** $32.5 \times 4\text{ КБ} = 130 \text{ КБ/сек}$ (1 Мбит/сек — меньше 1% от 100 Мбит канала VPS)

**Phase 3:** $455 \times 4\text{ КБ} = 1.8 \text{ МБ/сек}$ (14.4 Мбит/сек)

---

## 3. Реальная конфигурация Phase 1 (VPS 2–3 тыс. ₽)

### 3.1 Распределение памяти на VPS (8 ГБ RAM)

| Компонент | RAM (лимит) | Комментарий |
|---|---|---|
| **PostgreSQL 17** | 2 ГБ | `shared_buffers = 512MB`, `effective_cache_size = 1.5GB` |
| **Kafka KRaft** (1 брокер) | 1.5 ГБ | `KAFKA_HEAP_OPTS=-Xmx768m`, OS page cache для log segments |
| **Redis 7** (standalone) | 128 МБ | `maxmemory 128mb`, `maxmemory-policy volatile-lru` |
| **Keycloak 26** | 1 ГБ | `JAVA_OPTS=-Xmx512m` |
| **profile-service** | 512 МБ | `JAVA_OPTS=-Xmx384m -XX:MaxRAMPercentage=75` |
| **diary-service** | 512 МБ | |
| **catalog-price-service** | 512 МБ | |
| **recipe-ai-service** | 512 МБ | |
| **Schema Registry** | 256 МБ | `SCHEMA_REGISTRY_HEAP_OPTS=-Xmx128m` |
| **ОС + Docker daemon** | 1 ГБ | Kernel, buffers, container overhead |
| **Итого** | **≈ 7.9 ГБ** | Впритык, но работает |

> [!WARNING]
> При 8 ГБ RAM свободного места минимум. Если нужен `food-cv-service` (ML-инференс), понадобится VPS за 5 000 ₽ (16 ГБ RAM) или вынос ML на отдельный GPU-инстанс.

### 3.2 Docker Compose — конфигурация ресурсных лимитов для VPS

```yaml
# Добавить в docker-compose.local.yml для каждого сервиса:

services:
  postgres:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G

  redis:
    command: >
      redis-server
      --appendonly yes
      --maxmemory 128mb
      --maxmemory-policy volatile-ttl
      --save ""
    deploy:
      resources:
        limits:
          memory: 192M

  kafka:
    environment:
      KAFKA_HEAP_OPTS: "-Xmx768m -Xms512m"
      KAFKA_NUM_PARTITIONS: "3"              # default для авто-создаваемых топиков
      KAFKA_DEFAULT_REPLICATION_FACTOR: "1"
      KAFKA_LOG_RETENTION_HOURS: "168"        # 7 дней
      KAFKA_LOG_RETENTION_BYTES: "1073741824" # 1 ГБ макс на партицию
    deploy:
      resources:
        limits:
          memory: 1536M

  # Каждый Spring Boot сервис:
  profile-service:
    environment:
      JAVA_OPTS: "-Xmx384m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
    deploy:
      resources:
        limits:
          memory: 512M
```

### 3.3 Конфигурация Kafka на Phase 1

| Параметр | Значение | Обоснование |
|---|---|---|
| Брокеров | **1** (KRaft, без Zookeeper) | Один VPS — не от чего фейловериться |
| `num.partitions` | **3** | Запас на Phase 2, не создаёт overhead |
| `default.replication.factor` | **1** | Один брокер → RF > 1 невозможен |
| `log.retention.hours` | **168** (7 дней) | Достаточно для replay при сбое консьюмера |
| `log.segment.bytes` | **104857600** (100 МБ) | Сегменты удаляются быстрее при малом потоке |
| `auto.create.topics.enable` | **false** | Топики создаём явно через Init-скрипт |
| Producer: `acks` | **all** | Для 1 брокера = `acks=1`, но мы готовы к Phase 2 |
| Producer: `enable.idempotence` | **true** | Защита от дублей при retry |
| Producer: `max.in.flight.requests` | **5** | С идемпотентностью порядок гарантирован (sequence numbers) |
| Consumer: `auto.offset.reset` | **earliest** | При первом старте читаем всю историю |
| Consumer: `enable.auto.commit` | **false** | Ручной коммит после обработки |

### 3.4 Конфигурация Redis на Phase 1

| Параметр | Значение | Обоснование |
|---|---|---|
| Тип | **Standalone** | Один VPS, HA на этом уровне бессмысленна |
| `maxmemory` | **128mb** | 6.5 МБ данных, запас 20x |
| `maxmemory-policy` | **volatile-ttl** | Вытесняет только ключи с наименьшим TTL (кэш удалится раньше 24h идемпотентности) |
| `appendonly` | **yes** | Персистентность (идемпотентность переживает рестарт) |
| `appendfsync` | **everysec** | Баланс: максимум 1 сек потери при crash |
| `save` | **(отключен)** | RDB-снапшоты не нужны при AOF |
| Версия | **Redis 7 Alpine** | Минимальный footprint |

---

## 4. Анализ тезисов из видео «Шорткат» в контексте реального бюджета

### 4.1 Таблица: фразы из видео → наш ответ

| Тезис из видео | Суть вопроса | Ответ для Phase 1 (2–3 тыс. ₽ VPS) | Ответ для Phase 3 (Kubernetes) |
|---|---|---|---|
| **«Стратегия кеширования»** | Cache-Aside vs Write-Through vs Write-Behind | **Cache-Aside** с TTL. Идемпотентность — **Write-Through** (TTL 24ч в `RedisIdempotencyStore`). Каталог — Cache-Aside TTL 1ч. AI рецепты — Cache-Aside TTL 12ч. Мобильный клиент — Local-First Room KMP 2.8.x | То же + XFetch для hot keys, Single-Flight mutex, TTL jitter ±10% |
| **«Количество в бакеты максимальное»** | Token Bucket / Leaky Bucket параметры | На VPS: **Nginx rate limiting** (`limit_req zone=api burst=20 nodelay`) или простой interceptor в Spring. Redis не нужен для Rate Limiting при 2 RPS | Envoy Gateway + Redis Token Bucket: 100 req/min per `userId`, burst capacity 100 |
| **«Redis слишком нищий... кластер 3+3»** | Зачем кластер Redis | **Не нужен.** 6.5 МБ данных, 2 RPS. Standalone Redis с 128 МБ — запас 20x. Кластер оправдан при: (a) > 2 ГБ данных, (b) > 50 000 ops/sec, (c) требование HA 99.9%+ | Redis Cluster 3+3 (3 Master + 3 Replica), 16 384 hash slots, `volatile-lru` |
| **«Идемпотентность, гарантия доставки»** | At-least-once + дедупликация | Producer: `enable.idempotence=true`, `acks=all`. Consumer: дедупликация по `(userId, mealId)` через `UNIQUE` constraint в PostgreSQL. Transactional Outbox | То же + `min.insync.replicas=2` (при RF=3) |
| **«Ретрай и DLQ»** | Что делать с poison pills | Phase 1: Spring Simple Retry (3 попытки, экспоненциальный backoff). При исчерпании — логирование + алерт. DLT-топик существует, но обрабатывается вручную | Multi-topic Non-blocking Retry: `RETRY-1` (1с) → `RETRY-2` (10с) → `RETRY-3` (60с) → `DLT` |
| **«Недоступность даже Кафки»** | Что если Kafka умер | **Transactional Outbox.** gRPC-методы пишут в PostgreSQL, а событие кладут в `outbox_messages` в одной транзакции. Пользователь не замечает сбоя Kafka. OutboxProcessor ждёт восстановления | То же, но OutboxProcessor может быть заменён на Debezium CDC (WAL-based, zero polling) |
| **«Партиции и RPS»** | Сколько партиций нарезать | **3 партиции** на топик. При 0.84 msg/sec одна партиция (10 000 msg/sec capacity) загружена на 0.008%. 3 — запас для Phase 2 | 6–12 партиций, key = `userId` для FIFO на пользователя |

---

## 5. Глубокий разбор архитектурных вопросов (System Design)

### Вопрос 1: Cache Stampede (Thundering Herd) — актуален ли для нас?

**Проблема:** Популярный ключ кэша истекает по TTL. Сотни запросов одновременно видят Cache Miss и лавинообразно идут в БД/API.

**На Phase 1 — НЕ актуален.** При 2 RPS вероятность одновременного cache miss двух запросов к одному ключу ≈ 0.04%. Но архитектурно готовимся:

| Решение | Phase 1 | Phase 3 |
|---|---|---|
| **TTL Jitter** | ✅ Добавляем `± 10%` к базовому TTL | ✅ |
| **XFetch (вероятностное раннее обновление)** | ❌ Overkill | ✅ $\Delta \cdot \beta \cdot \ln(\text{rand}()) < -TTL_{remaining}$ |
| **Single-Flight Mutex** | ❌ | ✅ `SET key:mutex token NX PX 2000` |
| **Stale-While-Revalidate** | ❌ | ✅ Возврат устаревшего значения на 50 мс |

---

### Вопрос 2: Redis Cluster 3+3 vs Sentinel vs Standalone

| | **Standalone** | **Sentinel** | **Cluster 3+3** |
|---|---|---|---|
| **Шардирование** | ❌ Один узел | ❌ Один Master для записи | ✅ 16 384 hash slots, 3 мастера |
| **Отказоустойчивость** | ❌ SPOF | ✅ Автоматический failover | ✅ Кворумное голосование |
| **Масштабирование записи** | ❌ 1 ядро CPU | ❌ 1 ядро CPU | ✅ 3 ядра параллельно |
| **Мин. RAM** | 64 МБ | 3× (master + 2 sentinel) | 6× (3M + 3R) × 2 ГБ = 12 ГБ |
| **Подходит для** | Phase 1–2 | Не рекомендуется (хуже Cluster по всем метрикам, кроме простоты) | Phase 3+ |

**Правило хэш-тегов (`{...}`):** При переходе на Cluster ключи мультиключевых операций группируются тегами:
```
{user:123}:idempotency:abc   ← попадает в slot(hash("user:123"))
{user:123}:ratelimit:minute  ← тот же slot
```
Это позволяет выполнять Lua-скрипты и `MGET` атомарно на одном узле.

---

### Вопрос 3: Почему нельзя смешивать кэш и идемпотентность в одном Redis?

**На Phase 1 — смешиваем, но безопасно** благодаря `volatile-lru`:
- Все ключи кэша имеют TTL → попадают под LRU-вытеснение
- Ключи идемпотентности тоже имеют TTL (24ч) → тоже под LRU
- Но при `maxmemory 128mb` и 6.5 МБ данных вытеснение **никогда не сработает**

**На Phase 3** (> 500 МБ данных) — разделение на 2 логических Redis:
1. **Redis Cache** (`allkeys-lru`): каталог, рецепты. Допускается потеря.
2. **Redis State** (`noeviction`): идемпотентность, rate limit, locks. Недопустима потеря.

---

### Вопрос 4: Eviction Policy — что выбрать и почему

| Политика | Поведение | Когда использовать |
|---|---|---|
| `noeviction` | Ошибка `OOM` при записи, если память исчерпана | Когда потеря любого ключа недопустима |
| `allkeys-lru` | Вытесняет наименее используемый ключ из ВСЕХ | Чистый кэш, можно потерять любой ключ |
| `volatile-lru` | Вытесняет наименее используемый ключ **только из ключей с TTL** | Опасно для идемпотентности, если редкий ключ вытеснится раньше срока |
| `volatile-ttl` ✅ | Вытесняет ключ с наименьшим оставшимся TTL | Смешанная нагрузка (наш случай Phase 1–2: кэш каталога 1h удалится раньше ключа идемпотентности 24h) |

---

### Вопрос 5: Строгий порядок сообщений (FIFO) в Kafka при retries

**Проблема переупорядочивания:**
```
Продюсер: [Msg1] → [Msg2] → ...
Msg1 → FAIL (timeout)
Msg2 → SUCCESS (записан в партицию)
Msg1 → RETRY → SUCCESS (записан ПОСЛЕ Msg2)
Результат: [Msg2, Msg1] — порядок нарушен!
```

**Решение (наша конфигурация):**
1. `enable.idempotence = true` — брокер присваивает каждому продюсеру `ProducerId` (PID) и монотонный `SequenceNumber`
2. `max.in.flight.requests.per.connection = 5` — с идемпотентностью брокер отклонит Msg2 до прихода Msg1 (проверяет sequence numbers)
3. Партиционирование по `userId` — все события одного пользователя в одной партиции

**Формула проверки порядка на брокере:**

$$\text{if } SeqNum_{incoming} \neq SeqNum_{expected} \Rightarrow \text{OutOfOrderSequenceException}$$

Брокер откладывает запись до прихода ожидаемого sequence number.

---

### Вопрос 6: Dual-Write Problem — почему Kafka запрещён внутри @Transactional

```kotlin
// ❌ АНТИПАТТЕРН (запрещён в AGENTS.md §4.1)
@Transactional
fun addEntry(req: AddEntryRequest) {
    diaryRepository.save(entry)          // 1. INSERT в PostgreSQL
    kafkaTemplate.send("diary", event)   // 2. Сетевой вызов в Kafka
    // 3. COMMIT транзакции PostgreSQL
}
```

**Сценарии сбоя:**
- **Сбой А:** Kafka получил сообщение (шаг 2 OK), но PostgreSQL упал на COMMIT (шаг 3). В Kafka событие есть, в БД данных нет → **фантомное событие**.
- **Сбой Б:** PostgreSQL закоммичен (шаг 3 OK), но Kafka недоступен (шаг 2 FAIL). Данные в БД есть, события нет → **потерянное событие**.

**Решение:** Transactional Outbox. Шаги 1 и 2 происходят **в одной PostgreSQL-транзакции**. Kafka публикация — отдельный фоновый процесс.

---

### Вопрос 7: Bloat таблицы `outbox_messages`

**Проблема:** INSERT → UPDATE `status='PUBLISHED'` → DELETE. MVCC PostgreSQL создаёт dead tuples, autovacuum может не успевать.

| Решение | Phase 1 | Phase 3 |
|---|---|---|
| **Partial Index** | ✅ `CREATE INDEX idx_outbox_pending ON outbox_messages (created_at) WHERE status = 'PENDING'` | ✅ |
| **Периодический DELETE** | ✅ Cron job: `DELETE FROM outbox_messages WHERE status = 'PUBLISHED' AND published_at < NOW() - INTERVAL '3 days'` | ✅ |
| **Range Partitioning по дням** | ❌ Overkill при 6 000 записей/день | ✅ `DROP TABLE outbox_messages_old_partition` |
| **Debezium CDC** | ❌ Отдельный JVM-процесс, лишняя память | ✅ Читает WAL, без UPDATE статуса |

---

### Вопрос 8: Non-blocking Retry vs блокирующий retry

**Почему `Thread.sleep()` в Kafka Consumer — это катастрофа:**

```
Consumer.poll() → получаем Msg1 → Thread.sleep(10_000)
                                   ↓
                           Не шлём heartbeat
                                   ↓
                      max.poll.interval.ms exceeded
                                   ↓
                    Consumer Group REBALANCE 💥
                                   ↓
              ВСЕ консьюмеры группы перезапускаются
```

**На Phase 1:** Spring Simple Retry (3 попытки, backoff 500ms → 1s → 2s). При малом потоке задержка 3.5с не вызовет rebalance (default `max.poll.interval.ms = 300 000`). Но паттерн Multi-Topic Retry готов к активации:

```
[ diary.entry.created ] ──fail──→ [ .RETRY-1 (1с) ] ──fail──→ [ .RETRY-2 (10с) ] ──fail──→ [ .DLT ]
       │                                │                            │                        │
  (next msg)                       (next msg)                   (next msg)              (Alerting)
```

---

### Вопрос 9: Полная недоступность Kafka

**Поведение FitAssistent при падении Kafka:**

1. `OutboxProcessor.processOutbox()` ловит `KafkaException`, инкрементирует `attempts`, логирует WARN
2. gRPC-вызовы продолжают работать штатно: INSERT в `diary_entries` + INSERT в `outbox_messages` — обе в PostgreSQL
3. Пользователь **не замечает сбоя** — мобильное приложение получает успешный ответ
4. После восстановления Kafka `OutboxProcessor` вычитывает накопленные PENDING-сообщения и отправляет пачками по 100

**Время восстановления:** При 0.84 msg/sec и 2ч даунтайма накопится ~6 000 сообщений. При скорости отправки 100 msg/batch × 2 batch/sec = 200 msg/sec → **очередь разгрузится за 30 секунд**.

---

### Вопрос 10: Key Skew (перекос партиций)

**Проблема:** Один пользователь генерирует непропорционально много событий → его партиция перегружена.

**Phase 1:** Не актуально при 3 партициях и 500 DAU. Rate Limiting на уровне Nginx/Spring interceptor (100 req/min per userId) предотвращает абьюз.

**Phase 3:** Составной ключ `{userId}:{date}` или соль `userId + "_" + (hash % 4)` для распределения нагрузки между партициями (когда строгий порядок не нужен между днями).

---

### Вопрос 11: Schema Registry и режим совместимости BACKWARD

**Как работает:**
1. Продюсер сериализует Avro-сообщение → первые 5 байт: `[0x00][Schema ID: 4 bytes]`
2. Schema Registry хранит все версии схем
3. Консьюмер запрашивает схему по ID (кэширует локально)

**Режим BACKWARD (наш выбор):**
- ✅ Можно добавлять новые поля **с `default` значением**
- ✅ Можно удалять необязательные поля
- ❌ Нельзя удалять обязательные поля
- ❌ Нельзя менять тип поля

**Правило деплоя:** Сначала деплоим **консьюмер** (он умеет читать и старую, и новую схему), затем **продюсер** (начинает писать новую схему).

---

### Вопрос 12: Что если упал Redis?

**Phase 1 — Graceful Degradation:**

| Функция Redis | Fallback при недоступности | Последствия |
|---|---|---|
| **Кэш каталога** | Прямой запрос в PostgreSQL | Латентность +20 мс, нагрузка на БД |
| **Идемпотентность** | `UNIQUE(user_id, idempotency_key)` в PostgreSQL | Дубль отклонится базой, но ответ будет 500 вместо кэшированного 200 |
| **Rate Limiting** | `fail_open = true` (пропускаем все запросы) | Временно нет лимитов, но при 2 RPS это безопасно |
| **Кэш AI рецептов** | Запрос напрямую к LLM API | Высокая латентность (2–5с), расход квоты OpenAI |

**Реализация:** Resilience4j `@CircuitBreaker` на `RedisTemplate`. При 50% ошибок за окно в 10 запросов — Circuit открывается на 30 секунд, вызовы уходят в fallback.

---

## 6. Сводная матрица архитектурных решений

| Компонент | Phase 1 (2–3 тыс. ₽ VPS) | Phase 2 (5–10 тыс. ₽) | Phase 3 (Kubernetes) |
|---|---|---|---|
| **Redis** | Standalone, 128 МБ, `volatile-ttl` | Standalone, 512 МБ, `volatile-ttl` | Cluster 3+3, 2 ГБ/нода |
| **Kafka** | 1 брокер KRaft, RF=1 | 1 брокер, RF=1 | 3 брокера KRaft (Strimzi), RF=3 |
| **Партиции** | 3 на топик | 3–6 на топик | 6–12 на топик |
| **PostgreSQL** | 1 инстанс, `shared_buffers=512MB` | 1 инстанс, `shared_buffers=1GB` | HA (Patroni + pgBouncer) |
| **Outbox** | Scheduled Polling 500ms | Polling 250ms | Debezium CDC |
| **Retry/DLQ** | Spring Simple Retry (3 попытки) | Spring Simple Retry | Multi-topic Non-blocking + DLT |
| **Rate Limiting** | Nginx `limit_req` / Spring interceptor | Nginx + Redis Token Bucket | Envoy Gateway + Redis |
| **Мониторинг** | docker logs + Prometheus + Grafana | + AlertManager | + OTel Collector + Tempo + Loki |
| **Деплой** | `docker compose up -d` по SSH | + Watchtower / GitHub Actions | Helm + ArgoCD |

---

## 7. Чеклист перед запуском Phase 1

- [ ] VPS заказан (4 vCPU, 8 ГБ RAM, 80+ ГБ SSD, Ubuntu 24.04)
- [ ] Docker + Docker Compose установлены
- [ ] `docker-compose.local.yml` обновлён с resource limits
- [ ] Redis: `maxmemory 128mb`, `maxmemory-policy volatile-ttl`
- [ ] Kafka: `num.partitions=3`, `log.retention.hours=168`
- [ ] PostgreSQL: `shared_buffers=512MB`, `max_connections=100`
- [ ] Firewall: открыты только 443 (HTTPS/gRPC-TLS) и 22 (SSH)
- [ ] Nginx reverse proxy: TLS termination, `limit_req`, proxy_pass к gRPC-портам
- [ ] Prometheus + Grafana запущены (доп. 512 МБ RAM — возможно, на отдельном VPS или с бесплатным Grafana Cloud)
- [ ] Backup PostgreSQL: `pg_dump` cron → S3-compatible storage (Selectel, Yandex Object Storage)
- [ ] Мониторинг дискового пространства (alert при > 80%)

---

## 8. Когда масштабировать (триггеры перехода между фазами)

| Метрика | Phase 1 → Phase 2 | Phase 2 → Phase 3 |
|---|---|---|
| **CPU VPS** | Среднее > 60% за час | N/A (переход на K8s) |
| **RAM VPS** | Свободно < 500 МБ | N/A |
| **PostgreSQL connections** | HikariCP pool exhaustion > 5 раз/день | Connection pooling через pgBouncer |
| **Redis memory** | `used_memory` > 80% от `maxmemory` | `used_memory` > 1.5 ГБ |
| **Kafka Consumer Lag** | Lag > 10 000 сообщений в течение 1 часа | Lag > 100 000 |
| **gRPC p95 latency** | > 500 мс (SLA = 200 мс) | > 200 мс |
| **DAU** | > 2 000 | > 10 000 |


---
name: infrastructure-capacity-redis-kafka
description: Строгие алгоритмы, стратегии выбора и Best Practices для конфигурации микросервисов (gRPC + Coroutines), Redis и Kafka. Использовать при генерации кода для любого сервиса.
---

# AI Skill: Архитектурные стратегии и Best Practices

## 1. Стратегия потребления памяти (gRPC + Kotlin Coroutines)
Поскольку проект использует gRPC на базе Netty с Kotlin Coroutines, классическая модель "1 поток на запрос" не применяется. Упор делается на off-heap память (Direct Buffers).

**Алгоритм выбора лимитов для сервиса:**
* **Лимит контейнера:** `512M`
* **JVM Flags:** `-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=128m -Xss512k`
* **Best Practice генерации:**
    * Никогда не использовать блокирующие вызовы (`Thread.sleep()`, `RestTemplate`, JDBC).
    * Использовать только неблокирующие клиенты: `gRPC stubs (suspend)`, `WebClient`, `R2DBC` (если не используется паттерн Outbox через JPA/Hibernate — для Outbox с JPA выделять отдельный пул потоков `Dispatchers.IO`).
    * Для Netty gRPC принудительно очищать буферы: использовать `ReferenceCountUtil.release()` в кастомных интерцепторах, если происходит прямая работа с байт-буферами.

---

## 2. Стратегия и алгоритм выбора для Redis

### Правила и Best Practices (Генерировать конфигурации согласно этим пунктам)
1. **Разделяйте инстансы (Лучшая практика на будущее).**
   Никогда не смешивайте кэш и критичные бизнес-данные (блокировки, ключи идемпотентности, rate limits) в одном Redis. Если бюджет RAM позволяет (>= 16 ГБ), сгенерируйте `docker-compose` с двумя контейнерами:
    * `redis-cache`: политика `allkeys-lru`.
    * `redis-state`: политика `noeviction`.
2. **Компромисс для VPS 8 ГБ — используйте `volatile-ttl`.**
   Если два инстанса поднять невозможно, в `docker-compose` ставится один Redis с политикой `--maxmemory-policy volatile-ttl`.
    * *Логика:* При переполнении памяти Redis сначала вычистит кэш каталога (т.к. остаток жизни меньше часа) и не тронет 24-часовые ключи идемпотентности.
3. **Не ставьте TTL на критичные данные при `volatile-lru`.**
   Если (в крайнем случае) настроен `volatile-lru`, ключи идемпотентности должны сохраняться **без TTL** (вечно), а очищать их должен `@Scheduled` cron внутри приложения. Иначе Redis удалит их как "расходный материал".
4. **Алгоритм Fallback (Graceful Degradation):**
    * Все вызовы к Redis оборачивать в `try-catch` / Resilience4j Circuit Breaker.
    * При недоступности Redis: Кэш -> идем в БД; Идемпотентность -> полагаемся на `UNIQUE` в PostgreSQL; Rate Limit -> пропускаем трафик (fail-open).

---

## 3. Стратегия и алгоритм выбора для Apache Kafka

### 3.1. Стратегия партиционирования
* **Сразу закладывать 3 или 6 партиций.**
  Числа 3 и 6 удобно делятся на 2 и на 3. Это позволяет в будущем масштабировать консьюмеры (запускать 2 или 3 копии сервиса) без сложной операции перенарезки (repartitioning) топика.
* **Алгоритм выбора ключа (Partition Key):**
    * Если важен порядок событий пользователя -> использовать `userId` (String).
    * Если порядок не важен -> отправлять `null` (Kafka размажет Round-Robin) или генерировать рандомный UUID, чтобы избежать перекоса (Key Skew).

### 3.2. Настройки Offset, Lag и Rebalance (Best Practices)
При генерации конфигурации `@KafkaListener` или `application.yml` строго применять следующие настройки:

* **Управление смещением (Offsets):**
    * `enable.auto.commit: false` — Автокоммит строго запрещен.
    * Коммит выполняется **вручную** (`AckMode.MANUAL_IMMEDIATE` в Spring) только после успешного сохранения результата бизнес-операции в БД.
    * `auto.offset.reset: earliest` — Для новых консьюмер-групп всегда вычитывать историю с начала (если топик создан недавно), чтобы не потерять данные при холодном старте.
* **Управление Rebalance и блокировками:**
    * `max.poll.interval.ms: 300000` (5 минут). Если обработка одного сообщения (корутина) займет дольше, консьюмер будет выброшен из группы, что вызовет **Rebalance**.
    * **Best practice генерации кода:** Любые внешние вызовы внутри Kafka Listener (например, запрос к AI/LLM) должны оборачиваться в таймаут (`withTimeout(20_000)` в корутинах), чтобы гарантированно уложиться в `max.poll.interval.ms` и не спровоцировать "Stop-The-World" ребалансировку всей группы консьюмеров.
* **Управление Lag (Отставанием):**
    * `max.poll.records: 50` — Не брать слишком большие батчи за раз. Лучше чаще поллить по 50 сообщений и быстро коммитить, чем взять 500 и поймать Rebalance по таймауту.

### 3.3. Стратегия Transactional Outbox (Гарантия доставки)
* **Запрет Dual-Write:** Никогда не генерировать код, где `kafkaTemplate.send()` вызывается внутри метода с аннотацией `@Transactional`.
* **Паттерн генерации Outbox:**
    1. В `@Transactional` методе сохраняется бизнес-сущность.
    2. Там же создается запись в `outbox_messages` (`status = PENDING`).
    3. Отдельный класс `OutboxProcessor` использует `@Scheduled` (или Coroutines `ticker`) с шагом 500-1000 мс.
    4. Запрос выборки из БД обязан использовать блокировку для консистентности:
       `SELECT * FROM outbox_messages WHERE status = 'PENDING' ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED`
    5. После успешной публикации (ACK от Kafka) -> `UPDATE status = 'PUBLISHED'`.

### 3.4. Стратегия ретраев (Retry & DLT)
* При ошибке в `KafkaListener` (сеть, БД легла):
    * **Не блокировать поток:** `Thread.sleep` запрещен. В Spring Kafka использовать `DefaultErrorHandler` с `FixedBackOff` (например, 3 попытки с интервалом 1 сек).
    * При исчерпании попыток — перенаправлять сообщение (Poison Pill) в топик `*.DLT` (Dead Letter Topic) с помощью `DeadLetterPublishingRecoverer`, сдвинуть оффсет основной партиции дальше и залогировать `ERROR`.



