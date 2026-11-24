package dev.andre.nutritionist.profile.grpc.idempotency

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.profile.grpc.ProfileGrpcService
import dev.andre.nutritionist.profile.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.profile.grpc.security.SecurityGrpcInterceptor
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class RedisIdempotencyCircuitBreakerTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var profileGrpcService: ProfileGrpcService

    @Autowired
    private lateinit var grpcExceptionInterceptor: GrpcExceptionInterceptor

    @Autowired
    private lateinit var securityGrpcInterceptor: SecurityGrpcInterceptor

    @Autowired
    private lateinit var profileRepository: ProfileJpaRepository

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: ProfileServiceGrpcKt.ProfileServiceCoroutineStub

    @BeforeEach
    fun setUp() {
        profileRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        if (::channel.isInitialized) channel.shutdownNow()
        if (::server.isInitialized) server.shutdownNow()
    }

    @Test
    fun `tryAcquire gracefully degrades to Acquired when Redis throws connection error`() {
        @Suppress("UNCHECKED_CAST")
        val mockRedisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, ByteArray>

        @Suppress("UNCHECKED_CAST")
        val mockValueOps = mock(ValueOperations::class.java) as ValueOperations<String, ByteArray>

        `when`(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps)
        `when`(mockValueOps.setIfAbsent(anyString(), any(ByteArray::class.java), any(Duration::class.java)))
            .thenThrow(RedisConnectionFailureException("Redis connection refused (simulated outage)"))

        val store = RedisIdempotencyStore(mockRedisTemplate)
        val result = store.tryAcquire("test-key-redis-down", ByteArray(32) { 1 })

        // Graceful degradation: circuit breaker / fallback allows request to proceed
        assertTrue(result is IdempotencyResult.Acquired)
    }

    @Test
    fun `gRPC updateProfile succeeds when Redis is completely unavailable via circuit breaker fallback`() =
        runTest {
            @Suppress("UNCHECKED_CAST")
            val mockRedisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, ByteArray>

            @Suppress("UNCHECKED_CAST")
            val mockValueOps = mock(ValueOperations::class.java) as ValueOperations<String, ByteArray>

            `when`(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps)
            `when`(mockValueOps.setIfAbsent(anyString(), any(ByteArray::class.java), any(Duration::class.java)))
                .thenThrow(RedisConnectionFailureException("Redis connection timed out"))

            val failingStore = RedisIdempotencyStore(mockRedisTemplate)
            val interceptor = IdempotencyServerInterceptor(failingStore)

            val serverName = InProcessServerBuilder.generateName()
            server =
                InProcessServerBuilder
                    .forName(serverName)
                    .directExecutor()
                    .addService(
                        ServerInterceptors.intercept(
                            profileGrpcService,
                            grpcExceptionInterceptor,
                            interceptor,
                            securityGrpcInterceptor,
                        ),
                    ).build()
                    .start()

            channel =
                InProcessChannelBuilder
                    .forName(serverName)
                    .directExecutor()
                    .build()

            stub = ProfileServiceGrpcKt.ProfileServiceCoroutineStub(channel)

            val userId = "user_cb_fallback_1"
            val request =
                UpdateProfileRequest
                    .newBuilder()
                    .setUserId(userId)
                    .setHeightCm(180)
                    .setWeightKg(75.0)
                    .setGender(GenderProto.GENDER_MALE)
                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                    .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                    .setWeeklyBudget(5000.0)
                    .build()

            val headers =
                Metadata().apply {
                    put(SecurityGrpcInterceptor.X_USER_ID_HEADER, userId)
                    put(SecurityGrpcInterceptor.X_USER_ROLES_HEADER, "ROLE_USER")
                    put(IdempotencyServerInterceptor.IDEMPOTENCY_KEY_HEADER, "idemp-key-cb-test")
                }
            val userStub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))

            val response = userStub.updateProfile(request)
            assertEquals(userId, response.userId)
            assertEquals(180, response.heightCm)

            // Проверяем, что в базе профиль успешно сохранился
            val saved = profileRepository.findByUserId(userId)
            assertTrue(saved != null)
            assertEquals(180, saved?.heightCm)
        }
}
