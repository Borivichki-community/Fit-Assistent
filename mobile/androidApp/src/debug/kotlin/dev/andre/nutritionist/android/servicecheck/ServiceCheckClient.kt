package dev.andre.nutritionist.android.servicecheck

import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt.CatalogServiceCoroutineStub
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DiaryServiceGrpcKt.DiaryServiceCoroutineStub
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuRequest
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineStub
import dev.andre.nutritionist.android.auth.AuthConfig
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.android.servicecheck.health.HealthCheckRequest
import dev.andre.nutritionist.android.servicecheck.health.HealthCheckResponse
import dev.andre.nutritionist.android.servicecheck.health.HealthGrpc
import dev.andre.nutritionist.data.remote.grpc.AndroidProfileGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.interceptor.AuthClientInterceptor
import dev.andre.nutritionist.data.remote.grpc.interceptor.TraceparentClientInterceptor
import dev.andre.nutritionist.domain.model.UserId
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.flow.toList
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal class ServiceCheckClient(
    private val authRepository: AuthRepository,
    private val authInterceptor: AuthClientInterceptor,
    private val traceInterceptor: TraceparentClientInterceptor,
    private val authConfig: AuthConfig,
) {
    suspend fun health(port: Int): String = withChannel(port) { channel ->
        val response = HealthGrpc.newBlockingStub(channel)
            .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .check(HealthCheckRequest.getDefaultInstance())
        if (response.status != HealthCheckResponse.ServingStatus.SERVING) {
            throw Status.UNAVAILABLE.withDescription(response.status.name).asException()
        }
        response.status.name
    }

    suspend fun getProfile(port: Int): String {
        val userId = currentUser()
        return withChannel(port, authenticated = true) { channel ->
            AndroidProfileGrpcServiceClient(channel).getProfile(GetProfileRequest(userId.raw)).toString()
        }
    }

    suspend fun updateProfile(port: Int): String {
        val userId = currentUser()
        val request = UpdateProfileRequest(
            userId = userId.raw,
            heightCm = 175,
            weightKg = 70.0,
            gender = GenderProto.GENDER_MALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_MODERATE,
            goalType = GoalTypeProto.GOAL_TYPE_MAINTAIN,
            nutritionGoal = NutritionGoalProto(targetWeightKg = 70.0),
            weeklyBudget = 4_000.0,
        )
        return withChannel(port, authenticated = true) { channel ->
            AndroidProfileGrpcServiceClient(channel).updateProfile(request).toString()
        }
    }

    suspend fun addEntry(port: Int, entry: DiaryCheckEntry): String =
        diary(port, entry.addKey) { stub -> stub.addEntry(entry.request).toString() }

    suspend fun deleteEntry(port: Int, entry: DiaryCheckEntry): String =
        diary(port, entry.deleteKey) { stub ->
            stub.deleteMeal(DeleteMealRequest.newBuilder().setEntryId(entry.request.entryId).build())
            "entry_id = ${entry.request.entryId}"
        }

    suspend fun getDailyEntries(port: Int, date: LocalDate): String =
        diary(port) { stub ->
            val request = GetDailyEntriesRequest.newBuilder()
                .setDate(date.toString())
                .setTimeZone(TIME_ZONE)
                .build()
            val response = stub.getDailyEntries(request)
            "date = $date ($TIME_ZONE)\nentries_count = ${response.entriesCount}\n${response.toString().trim()}"
        }

    suspend fun getRange(port: Int, endDate: LocalDate): String =
        diary(port) { stub ->
            val startDate = endDate.minusDays(6)
            val request = GetRangeRequest.newBuilder()
                .setStartDate(startDate.toString())
                .setEndDate(endDate.toString())
                .setTimeZone(TIME_ZONE)
                .build()
            val response = stub.getRange(request)
            "range = $startDate / $endDate ($TIME_ZONE)\nentries_count = ${response.entriesCount}\n${response.toString().trim()}"
        }

    suspend fun searchProducts(port: Int): String {
        currentUser()
        return withChannel(port, authenticated = true) { channel ->
            val request = SearchProductsRequest.newBuilder().setPageSize(10).build()
            val response = CatalogServiceCoroutineStub(channel)
                .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .searchProducts(request)
            "products_count = ${response.productsCount}\n${response.toString().trim()}"
        }
    }

    suspend fun generateWeekMenu(gatewayUrl: String): String {
        val address = URI(gatewayUrl.trim())
        val host = address.host
        require(
            address.scheme.equals("https", ignoreCase = true) && host != null &&
                address.rawUserInfo == null && address.rawQuery == null && address.rawFragment == null &&
                (address.path.isEmpty() || address.path == "/") &&
                (address.port == -1 || address.port in 1..65535),
        ) { "Enter the HTTPS address of the test Envoy gateway" }
        currentUser()
        val gateway = OkHttpChannelBuilder.forAddress(host, if (address.port == -1) 443 else address.port)
            .useTransportSecurity()
            .intercept(authInterceptor, traceInterceptor)
        return withChannel(gateway) { channel ->
            val request = GenerateWeekMenuRequest.newBuilder()
                .setDailyCalories(2_000)
                .setWeeklyBudgetMinor(400_000)
                .build()
            val events = RecipeAiServiceCoroutineStub(channel)
                .withDeadlineAfter(MENU_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .generateWeekMenu(request)
                .toList()
            check(events.lastOrNull()?.hasFinal() == true) { "Stream ended without a final menu summary" }
            events.joinToString("\n") { it.toString().trim() }
        }
    }

    fun keycloakDiscovery(): String {
        val connection = URI(authConfig.discoveryUri.toString()).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = HTTP_TIMEOUT_MILLIS
            connection.readTimeout = HTTP_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "HTTP $code" }
            val document = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val issuer = document.getString("issuer")
            check(issuer.isNotBlank() && document.getString("authorization_endpoint").isNotBlank() &&
                document.getString("token_endpoint").isNotBlank()
            ) { "Incomplete OpenID Connect configuration" }
            return "HTTP $code\nissuer = $issuer"
        } finally {
            connection.disconnect()
        }
    }

    fun keycloakSession(): String = "sub = ${currentUser().raw}"

    private suspend fun <T> diary(
        port: Int,
        idempotencyKey: String? = null,
        operation: suspend (DiaryServiceCoroutineStub) -> T,
    ): T {
        val userId = currentUser()
        val metadata = Metadata().apply {
            put(USER_ID, userId.raw)
            idempotencyKey?.let { put(IDEMPOTENCY_KEY, it) }
        }
        return withChannel(port) { channel ->
            val stub = DiaryServiceCoroutineStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            operation(stub)
        }
    }

    private fun currentUser(): UserId =
        authRepository.getUserId()?.takeIf { authRepository.isAuthenticated() }
            ?: throw Status.UNAUTHENTICATED.withDescription("Sign in through FitAssistant first").asException()

    private suspend fun <T> withChannel(
        port: Int,
        authenticated: Boolean = false,
        operation: suspend (ManagedChannel) -> T,
    ): T {
        val builder = OkHttpChannelBuilder.forAddress(HOST, port).usePlaintext()
        if (authenticated) builder.intercept(authInterceptor, traceInterceptor)
        return withChannel(builder, operation)
    }

    private suspend fun <T> withChannel(
        builder: OkHttpChannelBuilder,
        operation: suspend (ManagedChannel) -> T,
    ): T {
        val channel = builder.build()
        return try {
            operation(channel)
        } finally {
            channel.shutdownNow()
        }
    }

    companion object {
        private const val HOST = "10.0.2.2"
        private const val TIME_ZONE = "UTC"
        private const val RPC_TIMEOUT_SECONDS = 8L
        private const val MENU_TIMEOUT_SECONDS = 60L
        private const val HTTP_TIMEOUT_MILLIS = 4000
        private val USER_ID = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
        private val IDEMPOTENCY_KEY = Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}
