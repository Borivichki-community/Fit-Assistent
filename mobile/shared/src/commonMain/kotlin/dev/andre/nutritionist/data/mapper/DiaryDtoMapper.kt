package dev.andre.nutritionist.data.mapper

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.CostSnapshotProto
import com.budgetntr.proto.diary.v1.DiaryEntryItemProto
import com.budgetntr.proto.diary.v1.EntrySourceTypeProto
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.diary.v1.GetRangeResponse
import com.budgetntr.proto.diary.v1.MoneyProto
import com.budgetntr.proto.diary.v1.NutritionSummaryProto
import com.budgetntr.proto.profile.v1.TimestampProto
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.Instant
import kotlin.math.roundToInt

object DiaryDtoMapper {

    fun MealRecord.toAddEntryRequest(): AddEntryRequest = AddEntryRequest(
        entryId = id.raw,
        mealType = mealType.name,
        recordedAt = TimestampProto(
            seconds = recordedAt.epochSeconds,
            nanos = recordedAt.nanosecondsOfSecond,
        ),
        sourceType = when (sourceType) {
            EntrySourceType.PRODUCT -> EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT
            EntrySourceType.RECIPE -> EntrySourceTypeProto.ENTRY_SOURCE_TYPE_RECIPE
            EntrySourceType.CUSTOM -> EntrySourceTypeProto.ENTRY_SOURCE_TYPE_CUSTOM
            EntrySourceType.UNSPECIFIED -> EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED
        },
        sourceId = sourceId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        nutrition = NutritionSummaryProto(
            calories = nutrition.calories.roundToInt(),
            proteins = nutrition.proteins,
            fats = nutrition.fats,
            carbs = nutrition.carbs,
        ),
        cost = CostSnapshotProto(
            total = MoneyProto(
                amountMinor = cost?.amountMinor ?: 0L,
                currencyCode = Money.CURRENCY_RUB,
            ),
        ),
    )

    fun DiaryEntryItemProto.toDomain(): MealRecord = MealRecord(
        id = MealRecordId(id),
        mealType = when (mealType.uppercase()) {
            "BREAKFAST" -> MealType.BREAKFAST
            "LUNCH" -> MealType.LUNCH
            "DINNER" -> MealType.DINNER
            "SNACK" -> MealType.SNACK
            else -> MealType.SNACK
        },
        recordedAt = Instant.fromEpochSeconds(recordedAt.seconds, recordedAt.nanos),
        sourceType = when (sourceType) {
            EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT -> EntrySourceType.PRODUCT
            EntrySourceTypeProto.ENTRY_SOURCE_TYPE_RECIPE -> EntrySourceType.RECIPE
            EntrySourceTypeProto.ENTRY_SOURCE_TYPE_CUSTOM -> EntrySourceType.CUSTOM
            EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED,
            EntrySourceTypeProto.UNRECOGNIZED,
            -> EntrySourceType.UNSPECIFIED
        },
        sourceId = sourceId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        nutrition = NutritionFacts(
            calories = nutrition.calories.toDouble(),
            proteins = nutrition.proteins,
            fats = nutrition.fats,
            carbs = nutrition.carbs,
        ),
        cost = if (cost.total.amountMinor > 0) Money.fromMinor(cost.total.amountMinor) else null,
        idempotencyKey = IdempotencyKey.generate(),
        syncStatus = SyncStatus.SYNCED,
    )

    fun toGetDailyEntriesRequest(
        date: String,
        timeZone: String = "UTC",
        targetUserId: UserId? = null,
    ): GetDailyEntriesRequest = GetDailyEntriesRequest(
        date = date,
        timeZone = timeZone,
        targetUserId = targetUserId?.raw?.takeIf { it.isNotBlank() },
    )

    fun toGetRangeRequest(
        fromIso: String,
        untilIso: String,
        timeZone: String = "UTC",
        targetUserId: UserId? = null,
    ): GetRangeRequest = GetRangeRequest(
        startDate = fromIso,
        endDate = untilIso,
        timeZone = timeZone,
        targetUserId = targetUserId?.raw?.takeIf { it.isNotBlank() },
    )

    fun GetRangeResponse.toDomain(): List<MealRecord> = entries.map { it.toDomain() }
}
