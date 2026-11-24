package dev.andre.nutritionist.diary.grpc.client

import com.budgetntr.proto.recipe.v1.CheckCoachAccessRequest
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import dev.andre.nutritionist.diary.config.DiaryClientConfiguration
import dev.andre.nutritionist.diary.config.DiaryClientProperties
import dev.andre.nutritionist.diary.grpc.interceptor.DiaryRequestContextInterceptor
import dev.andre.nutritionist.diary.grpc.security.DiaryAuthContext
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RecipeAccessClient(
    @Qualifier("recipeServiceChannel")
    recipeServiceChannel: ManagedChannel,
    private val properties: DiaryClientProperties,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    private val logger = LoggerFactory.getLogger(RecipeAccessClient::class.java)
    private val stub = RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineStub(recipeServiceChannel)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(DiaryClientConfiguration.RECIPE_ACCESS_CIRCUIT_BREAKER)

    suspend fun checkCoachAccess(
        coachId: String,
        studentId: String,
    ): Boolean {
        val recipeProps = properties.clients.recipe
        val authContext = DiaryRequestContextInterceptor.currentAuthContext()
        val rolesHeader =
            authContext.roles
                .joinToString(",")
                .ifBlank { DiaryAuthContext.ROLE_USER }
        val headers =
            Metadata().apply {
                put(USER_ID_HEADER, coachId)
                put(ROLES_HEADER, rolesHeader)
                if (recipeProps.devBypass) {
                    put(DEV_BYPASS_HEADER, "true")
                }
                DiaryRequestContextInterceptor.currentTraceparent()?.let { put(TRACEPARENT_HEADER, it) }
            }
        val request =
            CheckCoachAccessRequest
                .newBuilder()
                .setCoachId(coachId)
                .setStudentId(studentId)
                .build()

        return try {
            val response =
                circuitBreaker.executeSuspendFunction {
                    stub
                        .withDeadlineAfter(recipeProps.deadline.toMillis(), TimeUnit.MILLISECONDS)
                        .checkCoachAccess(request, headers)
                }
            response.hasAccess && response.canViewDiary
        } catch (error: CancellationException) {
            throw error
        } catch (error: CallNotPermittedException) {
            logger.warn(
                "Coach access check blocked by open circuit breaker '{}' for coachId={}, studentId={}",
                DiaryClientConfiguration.RECIPE_ACCESS_CIRCUIT_BREAKER,
                coachId,
                studentId,
            )
            throw Status.UNAVAILABLE
                .withDescription("Recipe access service is unavailable")
                .withCause(error)
                .asRuntimeException()
        } catch (error: StatusException) {
            throw mapUpstreamStatus(error.status, error, coachId, studentId)
        } catch (error: StatusRuntimeException) {
            throw mapUpstreamStatus(error.status, error, coachId, studentId)
        } catch (error: Exception) {
            logger.warn("Coach access check failed for coachId={}, studentId={}: {}", coachId, studentId, error.message)
            throw Status.UNAVAILABLE
                .withDescription("Recipe access service is unavailable")
                .withCause(error)
                .asRuntimeException()
        }
    }

    private fun mapUpstreamStatus(
        upstreamStatus: Status,
        cause: Throwable,
        coachId: String,
        studentId: String,
    ): StatusRuntimeException {
        if (upstreamStatus.code == Status.Code.PERMISSION_DENIED) {
            return Status.PERMISSION_DENIED
                .withDescription(upstreamStatus.description ?: "Coach access denied")
                .withCause(cause)
                .asRuntimeException()
        }
        logger.warn(
            "Upstream recipe-ai-service CheckCoachAccess failed (status={}) for coachId={}, studentId={}: {}",
            upstreamStatus.code,
            coachId,
            studentId,
            upstreamStatus.description,
        )
        return Status.UNAVAILABLE
            .withDescription("Recipe access service is unavailable")
            .withCause(cause)
            .asRuntimeException()
    }

    companion object {
        private val USER_ID_HEADER = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
        private val ROLES_HEADER = Metadata.Key.of("x-user-roles", Metadata.ASCII_STRING_MARSHALLER)
        private val DEV_BYPASS_HEADER = Metadata.Key.of("x-dev-bypass", Metadata.ASCII_STRING_MARSHALLER)
        private val TRACEPARENT_HEADER = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)
    }
}
