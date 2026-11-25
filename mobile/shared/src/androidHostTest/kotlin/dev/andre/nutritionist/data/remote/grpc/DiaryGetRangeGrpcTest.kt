package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.diary.v1.CostSnapshotProto
import com.budgetntr.proto.diary.v1.DiaryEntryItemProto
import com.budgetntr.proto.diary.v1.EntrySourceTypeProto
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.diary.v1.GetRangeResponse
import com.budgetntr.proto.diary.v1.MoneyProto
import com.budgetntr.proto.diary.v1.NutritionSummaryProto
import com.budgetntr.proto.profile.v1.TimestampProto
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiaryGetRangeGrpcTest {

    @Test
    fun `getRangeRequest and response roundtrip serialization preserves targetUserId`() {
        val withTarget = GetRangeRequest(
            startDate = "2026-11-01",
            endDate = "2026-11-07",
            timeZone = "Europe/Moscow",
            targetUserId = "student-99",
        )
        val decodedWithTarget = GetRangeRequestMarshaller.parse(
            GetRangeRequestMarshaller.stream(withTarget),
        )
        assertEquals(withTarget, decodedWithTarget)

        val withoutTarget = GetRangeRequest(
            startDate = "2026-11-01",
            endDate = "2026-11-07",
            timeZone = "UTC",
            targetUserId = null,
        )
        val decodedWithoutTarget = GetRangeRequestMarshaller.parse(
            GetRangeRequestMarshaller.stream(withoutTarget),
        )
        assertEquals(withoutTarget, decodedWithoutTarget)
        assertNull(decodedWithoutTarget.targetUserId)

        val response = GetRangeResponse(
            entries = listOf(
                DiaryEntryItemProto(
                    id = "entry-r1",
                    sourceType = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_RECIPE,
                    sourceId = "rec-7",
                    nameSnapshot = "Куриный суп",
                    weightGrams = 350.0,
                    nutrition = NutritionSummaryProto(calories = 280, proteins = 24.0, fats = 8.0, carbs = 22.0),
                    recordedAt = TimestampProto(seconds = 1793534400L, nanos = 0),
                    mealType = "LUNCH",
                    cost = CostSnapshotProto(total = MoneyProto(amountMinor = 14500L, currencyCode = "RUB")),
                ),
            ),
        )
        val decodedResp = GetRangeResponseMarshaller.parse(
            GetRangeResponseMarshaller.stream(response),
        )
        assertEquals(response, decodedResp)
    }

    @Test
    fun `getDailyEntriesRequest roundtrip serialization preserves optional targetUserId`() {
        val request = GetDailyEntriesRequest(
            date = "2026-11-24",
            timeZone = "Europe/Moscow",
            targetUserId = "student-55",
        )
        val decoded = GetDailyEntriesRequestMarshaller.parse(
            GetDailyEntriesRequestMarshaller.stream(request),
        )
        assertEquals(request, decoded)
    }

    @Test
    fun `DiaryGrpcRemoteDataSource getRange and getDailyEntries forward targetUserId over gRPC client`() = runTest {
        var capturedRangeReq: GetRangeRequest? = null
        var capturedDailyReq: GetDailyEntriesRequest? = null

        val fakeChannel = object : Channel() {
            override fun authority(): String = "localhost"

            override fun <ReqT, RespT> newCall(
                methodDescriptor: MethodDescriptor<ReqT, RespT>,
                callOptions: CallOptions,
            ): ClientCall<ReqT, RespT> {
                return object : ClientCall<ReqT, RespT>() {
                    private lateinit var listener: Listener<RespT>
                    private var rawBytes: ByteArray = ByteArray(0)

                    override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                        listener = responseListener
                    }

                    override fun request(numMessages: Int) {}
                    override fun cancel(message: String?, cause: Throwable?) {}

                    override fun sendMessage(message: ReqT) {
                        rawBytes = methodDescriptor.requestMarshaller.stream(message).readBytes()
                    }

                    override fun halfClose() {
                        listener.onHeaders(Metadata())
                        when (methodDescriptor.fullMethodName) {
                            "com.budgetntr.diary.v1.DiaryService/GetRange" -> {
                                val parsed = GetRangeRequestMarshaller.parse(rawBytes.inputStream())
                                capturedRangeReq = parsed
                                val respBytes = GetRangeResponseMarshaller.stream(
                                    GetRangeResponse(
                                        entries = listOf(
                                            DiaryEntryItemProto(
                                                id = "e-range",
                                                sourceType = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT,
                                                nameSnapshot = "Творог",
                                                weightGrams = 200.0,
                                                nutrition = NutritionSummaryProto(240, 32.0, 9.0, 6.0),
                                                recordedAt = TimestampProto(1793534400L, 0),
                                                mealType = "BREAKFAST",
                                                cost = CostSnapshotProto(MoneyProto(9000L, "RUB")),
                                            ),
                                        ),
                                    ),
                                )
                                listener.onMessage(methodDescriptor.responseMarshaller.parse(respBytes))
                            }
                            "com.budgetntr.diary.v1.DiaryService/GetDailyEntries" -> {
                                val parsed = GetDailyEntriesRequestMarshaller.parse(rawBytes.inputStream())
                                capturedDailyReq = parsed
                                val respBytes = GetDailyEntriesResponseMarshaller.stream(
                                    GetDailyEntriesResponse(),
                                )
                                listener.onMessage(methodDescriptor.responseMarshaller.parse(respBytes))
                            }
                        }
                        listener.onClose(Status.OK, Metadata())
                    }
                }
            }
        }

        val client = AndroidDiaryGrpcServiceClient(fakeChannel)
        val dataSource = DiaryGrpcRemoteDataSource(
            client = client,
            dispatchers = TestCoroutineDispatchers(),
            errorMapper = { GrpcErrorMapper.map(it) },
        )

        val rangeResult = dataSource.getRange(
            fromIso = "2026-11-01",
            untilIso = "2026-11-07",
            timeZone = "Europe/Moscow",
            targetUserId = UserId("student-coach-1"),
        )
        assertTrue(rangeResult.isSuccess)
        assertEquals(1, rangeResult.getOrThrow().size)
        assertEquals("e-range", rangeResult.getOrThrow().first().id.raw)
        assertEquals("student-coach-1", capturedRangeReq?.targetUserId)
        assertEquals("2026-11-01", capturedRangeReq?.startDate)
        assertEquals("2026-11-07", capturedRangeReq?.endDate)

        val dailyResult = dataSource.getDailyEntries(
            date = "2026-11-01",
            timeZone = "Europe/Moscow",
            targetUserId = UserId("student-coach-1"),
        )
        assertTrue(dailyResult.isSuccess)
        assertEquals("student-coach-1", capturedDailyReq?.targetUserId)
    }

    @Test
    fun `DiaryGrpcRemoteDataSource maps UNAUTHENTICATED and PERMISSION_DENIED via GrpcErrorMapper`() = runTest {
        var failWithStatus = Status.UNAUTHENTICATED.withDescription("Expired")

        val failingChannel = object : Channel() {
            override fun authority(): String = "localhost"
            override fun <ReqT, RespT> newCall(
                methodDescriptor: MethodDescriptor<ReqT, RespT>,
                callOptions: CallOptions,
            ): ClientCall<ReqT, RespT> {
                return object : ClientCall<ReqT, RespT>() {
                    private lateinit var listener: Listener<RespT>
                    override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                        listener = responseListener
                    }
                    override fun request(numMessages: Int) {}
                    override fun cancel(message: String?, cause: Throwable?) {}
                    override fun sendMessage(message: ReqT) {}
                    override fun halfClose() {
                        listener.onClose(failWithStatus, Metadata())
                    }
                }
            }
        }

        val dataSource = DiaryGrpcRemoteDataSource(
            client = AndroidDiaryGrpcServiceClient(failingChannel),
            dispatchers = TestCoroutineDispatchers(),
            errorMapper = { GrpcErrorMapper.map(it) },
        )

        val unauthResult = dataSource.getRange("2026-11-01", "2026-11-07", "UTC", null)
        assertTrue(unauthResult.isFailure)
        assertTrue(unauthResult.exceptionOrNull() is DomainError.Unauthorized)

        failWithStatus = Status.PERMISSION_DENIED.withDescription("Coach link revoked")
        val forbiddenResult = dataSource.getRange("2026-11-01", "2026-11-07", "UTC", UserId("student-1"))
        assertTrue(forbiddenResult.isFailure)
        assertTrue(forbiddenResult.exceptionOrNull() is DomainError.Forbidden)
    }
}
