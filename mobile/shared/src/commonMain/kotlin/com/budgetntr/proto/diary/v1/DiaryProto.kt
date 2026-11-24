package com.budgetntr.proto.diary.v1

import com.budgetntr.proto.profile.v1.TimestampProto

enum class EntrySourceTypeProto(val value: Int) {
    ENTRY_SOURCE_TYPE_UNSPECIFIED(0),
    ENTRY_SOURCE_TYPE_PRODUCT(1),
    ENTRY_SOURCE_TYPE_RECIPE(2),
    ENTRY_SOURCE_TYPE_CUSTOM(3),
    UNRECOGNIZED(-1);

    companion object {
        fun fromValue(value: Int): EntrySourceTypeProto =
            entries.firstOrNull { it.value == value } ?: UNRECOGNIZED
    }
}

data class MoneyProto(
    val amountMinor: Long = 0L,
    val currencyCode: String = "RUB",
)

data class CostSnapshotProto(
    val total: MoneyProto = MoneyProto(),
    val unitPrice: MoneyProto = MoneyProto(),
    val priceBasisMilligrams: Long? = null,
)

data class NutritionSummaryProto(
    val calories: Int = 0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0,
)

data class AddEntryRequest(
    val entryId: String = "",
    val mealType: String = "",
    val recordedAt: TimestampProto = TimestampProto(),
    val sourceType: EntrySourceTypeProto = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED,
    val sourceId: String? = null,
    val nameSnapshot: String = "",
    val weightGrams: Double = 0.0,
    val nutrition: NutritionSummaryProto = NutritionSummaryProto(),
    val cost: CostSnapshotProto = CostSnapshotProto(),
)

data class DiaryEntryItemProto(
    val id: String = "",
    val sourceType: EntrySourceTypeProto = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED,
    val sourceId: String? = null,
    val nameSnapshot: String = "",
    val weightGrams: Double = 0.0,
    val nutrition: NutritionSummaryProto = NutritionSummaryProto(),
    val recordedAt: TimestampProto = TimestampProto(),
    val mealType: String = "",
    val cost: CostSnapshotProto = CostSnapshotProto(),
)

data class AddEntryResponse(
    val entry: DiaryEntryItemProto = DiaryEntryItemProto(),
)

data class MealNutritionSummaryProto(
    val mealType: String = "",
    val nutrition: NutritionSummaryProto = NutritionSummaryProto(),
)

data class GetDailyEntriesRequest(
    val date: String = "",
    val timeZone: String = "UTC",
)

data class GetDailyEntriesResponse(
    val entries: List<DiaryEntryItemProto> = emptyList(),
    val dailyTotal: NutritionSummaryProto = NutritionSummaryProto(),
    val mealTotals: List<MealNutritionSummaryProto> = emptyList(),
)

data class DeleteMealRequest(
    val entryId: String = "",
)

data class DeleteMealResponse(
    val success: Boolean = true,
)
