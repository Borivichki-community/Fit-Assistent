package dev.andre.nutritionist.android.network

import android.util.Log
import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.CostSnapshot
import com.budgetntr.proto.diary.v1.DiaryServiceGrpcKt
import com.budgetntr.proto.diary.v1.Money
import com.budgetntr.proto.diary.v1.NutritionSummary
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DiaryEntry
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Реализация [DiaryRemoteDataSource] для Android, осуществляющая отправку
 * записей дневника в backend diary-service (gRPC на порту 9093).
 */
class AndroidDiaryRemoteDataSource(
    private val dispatchers: CoroutineDispatchers,
    private val channelProvider: () -> ManagedChannel = { defaultChannel() },
) : DiaryRemoteDataSource {

    private val userHeader = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
    private val keyHeader = Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)

    override suspend fun syncEntry(entry: DiaryEntry): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val userId = entry.userId.raw.ifBlank { "00000000-0000-0000-0000-000000000001" }
                val idempotencyKey = entry.idempotencyKey.raw.ifBlank { UUID.randomUUID().toString() }

                val headers = Metadata().apply {
                    put(userHeader, userId)
                    put(keyHeader, idempotencyKey)
                }

                val channel = channelProvider()
                val stub = DiaryServiceGrpcKt.DiaryServiceCoroutineStub(channel)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))

                val entryUuid = toValidUuid(entry.id.raw)
                val foodItemUuid = toValidUuid(entry.foodItemId.raw)

                val request = AddEntryRequest.newBuilder()
                    .setEntryId(entryUuid)
                    .setFoodItemId(foodItemUuid)
                    .setFoodName(entry.foodName)
                    .setQuantityGrams(entry.portionGrams)
                    .setMealType(entry.mealType.name)
                    .setNutrition(
                        NutritionSummary.newBuilder()
                            .setCalories(entry.calories)
                            .setProteins(entry.proteins)
                            .setFats(entry.fats)
                            .setCarbs(entry.carbs)
                            .build()
                    )
                    .setRecordedAt(
                        Timestamp.newBuilder()
                            .setSeconds(entry.recordedAt.epochSeconds)
                            .setNanos(entry.recordedAt.nanosecondsOfSecond)
                            .build()
                    )
                    .setCost(
                        CostSnapshot.newBuilder()
                            .setTotal(
                                Money.newBuilder()
                                    .setAmountMinor(25000)
                                    .setCurrencyCode("RUB")
                                    .build()
                            )
                            .build()
                    )
                    .build()

                Log.i("AndroidDiaryRemote", "Sending AddEntry to diary-service (entryId=$entryUuid, user=$userId, food='${entry.foodName}')...")
                val response = stub.addEntry(request)
                Log.i("AndroidDiaryRemote", "Received response from diary-service: created entryId=${response.entry.id}")
                Unit
            }
        }

    private fun toValidUuid(raw: String): String {
        return runCatching {
            UUID.fromString(raw).toString()
        }.getOrElse {
            UUID.nameUUIDFromBytes(raw.toByteArray()).toString()
        }
    }

    companion object {
        @Volatile
        private var sharedChannel: ManagedChannel? = null

        fun defaultChannel(): ManagedChannel {
            return sharedChannel ?: synchronized(this) {
                sharedChannel ?: run {
                    val host = resolveDiaryHost()
                    Log.i("AndroidDiaryRemote", "Connecting to diary-service at $host:9093")
                    ManagedChannelBuilder.forAddress(host, 9093)
                        .usePlaintext()
                        .build()
                        .also { sharedChannel = it }
                }
            }
        }

        private fun resolveDiaryHost(): String {
            // Android emulator accesses host 127.0.0.1 via 10.0.2.2
            return if (isEmulator()) "10.0.2.2" else "127.0.0.1"
        }

        private fun isEmulator(): Boolean {
            val fp = android.os.Build.FINGERPRINT
            val model = android.os.Build.MODEL
            val hw = android.os.Build.HARDWARE
            return fp.startsWith("generic") || fp.startsWith("unknown") ||
                model.contains("google_sdk") || model.contains("Emulator") ||
                hw.contains("goldfish") || hw.contains("ranchu")
        }
    }
}
