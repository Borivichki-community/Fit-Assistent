package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.AbstractPostgresIntegrationTest
import dev.andre.nutritionist.profile.grpc.idempotency.IdempotencyServerInterceptor
import dev.andre.nutritionist.profile.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.profile.grpc.security.AuthContext
import dev.andre.nutritionist.profile.grpc.security.SecurityGrpcInterceptor
import dev.andre.nutritionist.profile.grpc.security.currentAuthContext
import dev.andre.nutritionist.profile.grpc.security.currentCoroutineAuthContext
import dev.andre.nutritionist.profile.outbox.model.OutboxStatus
import dev.andre.nutritionist.profile.outbox.repository.OutboxJpaRepository
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.kotlin.GrpcContextElement
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ConcurrentLinkedQueue

class ProfileTransactionThreadSafetyTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var profileGrpcService: ProfileGrpcService

    @Autowired
    private lateinit var grpcExceptionInterceptor: GrpcExceptionInterceptor

    @Autowired
    private lateinit var idempotencyServerInterceptor: IdempotencyServerInterceptor

    @Autowired
    private lateinit var securityGrpcInterceptor: SecurityGrpcInterceptor

    @Autowired
    private lateinit var profileRepository: ProfileJpaRepository

    @Autowired
    private lateinit var outboxRepository: OutboxJpaRepository

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: ProfileServiceGrpcKt.ProfileServiceCoroutineStub

    @BeforeEach
    fun setUp() {
        outboxRepository.deleteAll()
        profileRepository.deleteAll()

        val serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(
                    ServerInterceptors.intercept(
                        profileGrpcService,
                        grpcExceptionInterceptor,
                        idempotencyServerInterceptor,
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
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    @Test
    fun `concurrent updateProfile calls maintain transactional integrity and atomic outbox creation`() =
        runTest {
            val concurrency = 10
            val errors = ConcurrentLinkedQueue<Throwable>()

            coroutineScope {
                val jobs =
                    (1..concurrency).map { index ->
                        async {
                            val userId = "user_concurrent_$index"
                            val request =
                                UpdateProfileRequest
                                    .newBuilder()
                                    .setUserId(userId)
                                    .setHeightCm(170 + index)
                                    .setWeightKg(60.0 + index)
                                    .setGender(GenderProto.GENDER_FEMALE)
                                    .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_LIGHT)
                                    .setGoalType(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT)
                                    .setWeeklyBudget(3000.0 + index * 100)
                                    .build()

                            val headers =
                                Metadata().apply {
                                    put(SecurityGrpcInterceptor.X_USER_ID_HEADER, userId)
                                    put(SecurityGrpcInterceptor.X_USER_ROLES_HEADER, "ROLE_USER")
                                }
                            val userStub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))

                            try {
                                val response = userStub.updateProfile(request)
                                assertEquals(userId, response.userId)
                            } catch (t: Throwable) {
                                errors.add(t)
                            }
                        }
                    }

                jobs.awaitAll()
            }

            assertTrue(errors.isEmpty(), "Expected no errors during concurrent executions, but got: $errors")
            assertEquals(concurrency.toLong(), profileRepository.count())
            assertEquals(concurrency.toLong(), outboxRepository.countByStatus(OutboxStatus.PENDING))
        }

    @Test
    fun `auth context is preserved across coroutine suspension and thread boundaries with GrpcContextElement`() =
        runTest {
            val grpcCtx =
                io.grpc.Context.current().withValue(
                    AuthContext.AUTH_CONTEXT_KEY,
                    AuthContext(userId = "coroutine_user_1", roles = setOf("ROLE_USER"), isAuthenticated = true),
                )

            grpcCtx.run {
                runBlocking {
                    withContext(GrpcContextElement.current()) {
                        val authFromThreadLocal = currentAuthContext()
                        val authFromCoroutineContext = currentCoroutineAuthContext()

                        assertEquals("coroutine_user_1", authFromThreadLocal.userId)
                        assertTrue(authFromThreadLocal.isAuthenticated)
                        assertEquals("coroutine_user_1", authFromCoroutineContext.userId)
                        assertTrue(authFromCoroutineContext.isAuthenticated)
                    }
                }
            }
        }
}
