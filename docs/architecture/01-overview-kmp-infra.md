# 01 — KMP-архитектура & Инфраструктура

> **Проект:** «Бюджетный нутрициолог»
> **Стек:** Kotlin 2.4.x · KMP · Room KMP 2.8.x · gRPC · Koin · Coroutines/Flow · Gradle 9.x

---

## 1. Структура `:shared` — Clean Architecture

```
:shared/
├── commonMain/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ids.kt                  # @JvmInline value class UserId, RecipeId, ...
│   │   │   ├── Recipe.kt
│   │   │   ├── DiaryEntry.kt
│   │   │   ├── FoodItem.kt
│   │   │   ├── UserProfile.kt
│   │   │   └── NutritionFacts.kt
│   │   ├── repository/
│   │   │   ├── RecipeRepository.kt
│   │   │   ├── DiaryRepository.kt
│   │   │   ├── FoodCatalogRepository.kt
│   │   │   └── ProfileRepository.kt
│   │   ├── usecase/
│   │   │   ├── recipe/
│   │   │   │   ├── GetRecipeUseCase.kt
│   │   │   │   ├── SearchRecipesUseCase.kt
│   │   │   │   └── GetBudgetRecipesUseCase.kt
│   │   │   ├── diary/
│   │   │   │   ├── AddDiaryEntryUseCase.kt
│   │   │   │   ├── GetDailyDiaryUseCase.kt
│   │   │   │   └── SyncPendingDiaryUseCase.kt
│   │   │   └── profile/
│   │   │       ├── GetProfileUseCase.kt
│   │   │       └── UpdateProfileUseCase.kt
│   │   └── coroutines/
│   │       └── CoroutineDispatchers.kt  # interface
│   ├── data/
│   │   ├── local/
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt       # @Database RoomDatabase
│   │   │   │   └── migrations/
│   │   │   ├── dao/
│   │   │   │   ├── RecipeDao.kt
│   │   │   │   ├── DiaryDao.kt
│   │   │   │   └── FoodItemDao.kt
│   │   │   └── entity/
│   │   │       ├── RecipeEntity.kt
│   │   │       ├── DiaryEntryEntity.kt
│   │   │       └── FoodItemEntity.kt
│   │   ├── remote/
│   │   │   ├── grpc/
│   │   │   │   ├── RecipeGrpcDataSource.kt
│   │   │   │   ├── DiaryGrpcDataSource.kt
│   │   │   │   └── ProfileGrpcDataSource.kt
│   │   │   └── channel/
│   │   │       └── GrpcChannelFactory.kt    # interface (платформенная реализация)
│   │   ├── mapper/
│   │   │   ├── RecipeMapper.kt         # DTO ↔ Domain ↔ Entity
│   │   │   ├── DiaryMapper.kt
│   │   │   └── FoodItemMapper.kt
│   │   └── repository/
│   │       ├── RecipeRepositoryImpl.kt
│   │       ├── DiaryRepositoryImpl.kt
│   │       └── ProfileRepositoryImpl.kt
│   └── di/
│       ├── DomainModule.kt             # Koin: UseCases
│       ├── DataModule.kt               # Koin: Repositories, DataSources
│       └── SharedModule.kt             # Koin: CoroutineDispatchers, DB
├── androidMain/
│   ├── di/
│   │   └── AndroidModule.kt            # Koin: Android-реализации интерфейсов
│   ├── coroutines/
│   │   └── AndroidCoroutineDispatchers.kt
│   ├── channel/
│   │   └── OkHttpGrpcChannelFactory.kt
│   ├── auth/
│   │   └── AppAuthTokenProvider.kt
│   ├── db/
│   │   └── AndroidDatabaseBuilder.kt
│   └── sync/
│       └── WorkManagerSyncScheduler.kt
├── iosMain/
│   ├── di/
│   │   └── IosModule.kt
│   ├── coroutines/
│   │   └── IosCoroutineDispatchers.kt
│   └── channel/
│       └── GrpcNioChannelFactory.kt     # grpc-swift или gRPCKotlin-NIO
└── commonTest/
    ├── domain/
    │   └── usecase/
    │       ├── GetRecipeUseCaseTest.kt
    │       └── AddDiaryEntryUseCaseTest.kt
    ├── data/
    │   ├── dao/
    │   │   └── DiaryDaoTest.kt          # BundledSQLiteDriver in-memory
    │   └── repository/
    │       └── RecipeRepositoryImplTest.kt
    └── fake/
        ├── FakeRecipeRepository.kt
        ├── FakeDiaryRepository.kt
        └── TestCoroutineDispatchers.kt
```

---

## 2. Правила Kotlin в `:shared`

### 2.1 Dependency Graph (строго)
```
domain ← data ← (androidMain / iosMain)
domain ← di
```
- `domain` не импортирует ничего из `data`, `androidMain`, `iosMain`.
- `data` знает о `domain` (реализует интерфейсы), но `domain` не знает о `data`.

### 2.2 Value Classes (реестр)
```kotlin
// :shared/commonMain/domain/model/ids.kt
package com.budgetntr.shared.domain.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline value class UserId(val raw: String)
@JvmInline value class RecipeId(val raw: String)
@JvmInline value class DiaryEntryId(val raw: String)
@JvmInline value class FoodItemId(val raw: String)
@JvmInline value class ProfileId(val raw: String)
@JvmInline value class IdempotencyKey(val raw: String) {
    companion object {
        fun generate(): IdempotencyKey = IdempotencyKey(Uuid.random().toString())
    }
}
```

### 2.3 CoroutineDispatchers
```kotlin
// commonMain
interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

// androidMain
class AndroidCoroutineDispatchers : CoroutineDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

// commonTest/fake
class TestCoroutineDispatchers(
    testDispatcher: TestCoroutineDispatcher = StandardTestDispatcher(),
) : CoroutineDispatchers {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
```

---

## 3. Koin DI — структура модулей

### 3.1 SharedModule (commonMain)
```kotlin
val sharedModule = module {
    // CoroutineDispatchers — платформа переопределяет через androidModule / iosModule
    single<CoroutineDispatchers> { AndroidCoroutineDispatchers() } // default, overridden
}

val domainModule = module {
    factory { GetRecipeUseCase(get(), get()) }
    factory { SearchRecipesUseCase(get(), get()) }
    factory { GetBudgetRecipesUseCase(get(), get()) }
    factory { AddDiaryEntryUseCase(get(), get()) }
    factory { GetDailyDiaryUseCase(get(), get()) }
    factory { SyncPendingDiaryUseCase(get(), get()) }
    factory { GetProfileUseCase(get(), get()) }
    factory { UpdateProfileUseCase(get(), get()) }
}

val dataModule = module {
    single<RecipeRepository> { RecipeRepositoryImpl(get(), get(), get()) }
    single<DiaryRepository> { DiaryRepositoryImpl(get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single<FoodCatalogRepository> { FoodCatalogRepositoryImpl(get(), get()) }
}
```

### 3.2 AndroidModule (androidMain)
```kotlin
val androidModule = module {
    single<CoroutineDispatchers> { AndroidCoroutineDispatchers() }
    single<GrpcChannelFactory> { OkHttpGrpcChannelFactory(get()) }
    single<AuthTokenProvider> { AppAuthTokenProvider(androidContext()) }
    single<SyncScheduler> { WorkManagerSyncScheduler(androidContext()) }
    single { buildDatabase(androidContext()) } // AppDatabase через AndroidDatabaseBuilder
}
```

---

## 4. Room KMP 2.8.x — использование

### 4.1 Подключение зависимостей (`libs.versions.toml`)
```toml
[versions]
room = "2.8.0"
sqlite-bundled = "2.5.0"

[libraries]
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite-bundled" }

[plugins]
room = { id = "androidx.room", version.ref = "room" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 4.2 Конфигурация Room KMP в `build.gradle.kts` `:shared`
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
    }
}

dependencies {
    // KSP для каждого таргета
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
```

### 4.3 Паттерн Entity → DAO → Repository
```kotlin
// Entity (не попадает в domain!)
@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey val id: String,            // хранится как String, в domain — DiaryEntryId
    val idempotencyKey: String,
    val foodItemId: String,
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val recordedAt: Long,                  // epochMillis → kotlinx-datetime Instant в domain
    val syncStatus: String,                // "PENDING" | "SYNCED" | "FAILED"
)

// DAO
@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE syncStatus = 'PENDING'")
    fun observePending(): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: DiaryEntryEntity)

    @Query("UPDATE diary_entries SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
```

---

## 5. gRPC в KMP

### 5.1 Подход
- Protobuf-файлы хранятся в отдельном Gradle-модуле `:proto` или в корне `proto/`.
- `buf generate` генерирует Java/Kotlin-стабы.
- В `commonMain` работаем только с интерфейсами Remote DataSource.
- Стаб gRPC используется только в `androidMain` / `iosMain`.

### 5.2 GrpcChannelFactory
```kotlin
// commonMain — интерфейс
interface GrpcChannelFactory {
    fun createChannel(host: String, port: Int): Any // платформенный тип
}

// androidMain — реализация
class OkHttpGrpcChannelFactory(
    private val authTokenProvider: AuthTokenProvider,
) : GrpcChannelFactory {
    override fun createChannel(host: String, port: Int): ManagedChannel =
        ManagedChannelBuilder
            .forAddress(host, port)
            .intercept(AuthInterceptor(authTokenProvider))
            .build()
}
```

### 5.3 AuthInterceptor (передача JWT + idempotency-key)
```kotlin
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : ClientInterceptor {
    override fun <Req, Resp> interceptCall(
        method: MethodDescriptor<Req, Resp>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<Req, Resp> {
        val token = tokenProvider.getAccessToken()
        val headers = Metadata().apply {
            put(
                Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer $token",
            )
        }
        return ForwardingClientCall.SimpleForwardingClientCall(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<Resp>, existingHeaders: Metadata) {
                existingHeaders.merge(headers)
                super.start(responseListener, existingHeaders)
            }
        }
    }
}
```

---

## 6. Инфраструктура — `infrastructure/docker-compose.local.yml`

```yaml
# infrastructure/docker-compose.local.yml
version: "3.9"

services:

  postgres:
    image: postgres:17-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_PASSWORD: local_pass
    volumes:
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports: ["2181:2181"]

  kafka:
    image: confluentinc/cp-kafka:3.9.0
    depends_on: [zookeeper]
    ports: ["9092:9092", "29092:29092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s

  schema-registry:
    image: confluentinc/cp-schema-registry:7.7.0
    depends_on: [kafka]
    ports: ["8081:8081"]
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:29092
      SCHEMA_REGISTRY_SCHEMA_COMPATIBILITY_LEVEL: BACKWARD

  keycloak:
    image: quay.io/keycloak/keycloak:26.0.0
    ports: ["8080:8080"]
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak_db
      KC_DB_USERNAME: postgres
      KC_DB_PASSWORD: local_pass
      KC_HOSTNAME: localhost
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    command: start-dev
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

### `infrastructure/init-db.sql` (начальная инициализация баз)
```sql
CREATE DATABASE profile_db;
CREATE DATABASE catalog_db;
CREATE DATABASE diary_db;
CREATE DATABASE keycloak_db;
```

---

## 7. Multistage Dockerfile (Spring Boot сервисы)

```dockerfile
# infrastructure/docker/Dockerfile.backend
# Stage 1 — Build
FROM gradle:8-jdk21 AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
# Download dependencies layer (cached)
RUN ./gradlew dependencies --no-daemon --quiet || true
COPY server/ server/
RUN ./gradlew :server:bootJar --no-daemon -x test

# Stage 2 — Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/server/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080 9090
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
```

---

## 8. Helm-чарты и Kubernetes-манифесты

### 8.1 Структура чарта
```
infrastructure/helm/
├── Chart.yaml
├── values.yaml
├── values-staging.yaml
├── values-prod.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── hpa.yaml                      # HorizontalPodAutoscaler
    ├── pdb.yaml                      # PodDisruptionBudget
    ├── grpcroute.yaml                # GRPCRoute (Gateway API)
    ├── security-policy.yaml          # SecurityPolicy (JWT via Envoy Gateway)
    ├── backend-traffic-policy.yaml   # BackendTrafficPolicy (rate limit)
    └── _helpers.tpl
```

### 8.2 Пример `values.yaml`
```yaml
replicaCount: 2

image:
  repository: gcr.io/budget-nutritionist
  pullPolicy: IfNotPresent
  tag: ""

service:
  type: ClusterIP
  httpPort: 8080
  grpcPort: 9090

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
```

---

## 9. CI/CD Pipeline

### 9.1 `build.yml` — сборка и проверки
```yaml
# .github/workflows/build.yml
name: Build & Quality Gates

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  kotlin-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Gradle Cache
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.toml', '**/*.kts') }}
      - name: ktlint
        run: ./gradlew ktlintCheck
      - name: detekt
        run: ./gradlew detekt
      - name: Buf lint
        uses: bufbuild/buf-action@v1
        with:
          lint: true
          breaking: true
          breaking_against: 'https://github.com/${{ github.repository }}.git#branch=main'
      - name: Unit Tests (shared)
        run: ./gradlew :shared:allTests
      - name: Unit Tests (server)
        run: ./gradlew :server:test
      - name: Build APK (debug)
        run: ./gradlew :app:androidApp:assembleDebug
      - name: Trivy vulnerability scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          severity: 'CRITICAL,HIGH'

  docker-build:
    runs-on: ubuntu-latest
    needs: kotlin-quality
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -f infrastructure/docker/Dockerfile.backend -t budget-nutritionist:${{ github.sha }} .
      - name: Trivy scan Docker image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'budget-nutritionist:${{ github.sha }}'
          severity: 'CRITICAL,HIGH'
```

### 9.2 `deploy.yml` — деплой в Kubernetes
```yaml
# .github/workflows/deploy.yml
name: Deploy to Kubernetes

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - name: Set up Helm
        uses: azure/setup-helm@v4
      - name: Configure kubectl
        uses: azure/k8s-set-context@v4
        with:
          kubeconfig: ${{ secrets.KUBE_CONFIG }}
      - name: Deploy with Helm
        run: |
          helm upgrade --install budget-nutritionist \
            ./infrastructure/helm \
            --namespace budget-nutritionist \
            --create-namespace \
            --values ./infrastructure/helm/values-prod.yaml \
            --set image.tag=${{ github.sha }} \
            --wait \
            --timeout 10m
      - name: Verify rollout
        run: kubectl rollout status deployment/budget-nutritionist -n budget-nutritionist
```

### 9.3 Проверка качества — инструменты
| Инструмент | Назначение | Конфигурация |
|---|---|---|
| `ktlint` | Форматирование Kotlin | `.editorconfig` |
| `detekt` | Статический анализ | `detekt.yml` (запрет `!!`, сложность) |
| `Buf` | Lint + Breaking change proto | `buf.yaml`, `buf.gen.yaml` |
| `Trivy` | Уязвимости в зависимостях и образах | GitHub Action |
| `Dependabot` | Автообновление зависимостей | `.github/dependabot.yml` |
