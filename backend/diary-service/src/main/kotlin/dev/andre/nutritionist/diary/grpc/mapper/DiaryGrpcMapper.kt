package dev.andre.nutritionist.diary.grpc.mapper

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.DiaryEntryProto
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.diary.domain.model.AddEntryCommand
import dev.andre.nutritionist.diary.domain.model.CostSnapshot
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.FoodItemId
import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.MealNutritionSummary
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.Money
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import dev.andre.nutritionist.diary.domain.model.UserId
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Instant
import com.budgetntr.proto.diary.v1.CostSnapshot as CostSnapshotProto
import com.budgetntr.proto.diary.v1.MealNutritionSummary as MealNutritionSummaryProto
import com.budgetntr.proto.diary.v1.Money as MoneyProto
import com.budgetntr.proto.diary.v1.NutritionSummary as NutritionSummaryProto

@Component
class DiaryGrpcMapper {
    fun toCommand(
        request: AddEntryRequest,
        userId: UserId,
        key: IdempotencyKey,
    ): AddEntryCommand =
        AddEntryCommand(
            entryId = DiaryEntryId(UUID.fromString(request.entryId)),
            userId = userId,
            idempotencyKey = key,
            foodItemId = FoodItemId(UUID.fromString(request.foodItemId)),
            foodName = request.foodName,
            quantityGrams = request.quantityGrams,
            nutrition =
                NutritionSummary(
                    request.nutrition.calories,
                    request.nutrition.proteins,
                    request.nutrition.fats,
                    request.nutrition.carbs,
                ),
            mealType = MealType.valueOf(request.mealType),
            recordedAt = Instant.fromEpochSeconds(request.recordedAt.seconds, request.recordedAt.nanos),
            cost =
                request.takeIf { it.hasCost() }?.cost?.let {
                    CostSnapshot(
                        total = Money(it.total.amountMinor),
                        unitPrice = if (it.hasUnitPrice()) Money(it.unitPrice.amountMinor) else null,
                        priceBasisMilligrams = it.takeIf { cost -> cost.hasPriceBasisMilligrams() }?.priceBasisMilligrams,
                    )
                },
        )

    fun toProto(entry: DiaryEntry): DiaryEntryProto {
        val builder =
            DiaryEntryProto
                .newBuilder()
                .setId(entry.id.raw.toString())
                .setFoodItemId(entry.foodItemId.raw.toString())
                .setFoodName(entry.foodName)
                .setQuantityGrams(entry.quantityGrams)
                .setNutrition(toProto(entry.nutrition))
                .setRecordedAt(
                    Timestamp
                        .newBuilder()
                        .setSeconds(entry.recordedAt.epochSeconds)
                        .setNanos(entry.recordedAt.nanosecondsOfSecond),
                ).setMealType(entry.mealType.name)
        entry.cost?.let { cost ->
            val snapshot = CostSnapshotProto.newBuilder().setTotal(toProto(cost.total))
            cost.unitPrice?.let { snapshot.setUnitPrice(toProto(it)) }
            cost.priceBasisMilligrams?.let(snapshot::setPriceBasisMilligrams)
            builder.setCost(snapshot)
        }
        return builder.build()
    }

    fun toProto(nutrition: NutritionSummary): NutritionSummaryProto =
        NutritionSummaryProto
            .newBuilder()
            .setCalories(nutrition.calories)
            .setProteins(nutrition.proteins)
            .setFats(nutrition.fats)
            .setCarbs(nutrition.carbs)
            .build()

    fun toProto(meal: MealNutritionSummary): MealNutritionSummaryProto =
        MealNutritionSummaryProto
            .newBuilder()
            .setMealType(meal.mealType.name)
            .setNutrition(toProto(meal.nutrition))
            .build()

    fun dateBounds(
        startDate: String,
        endDate: String,
        timeZone: String,
    ): Pair<Instant, Instant> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        require(start.year in 1..9999 && end.year in 1..9999 && start <= end) { "Invalid date range" }
        val zone = ZoneId.of(timeZone)
        val from = start.atStartOfDay(zone).toInstant()
        val until = end.plusDays(1).atStartOfDay(zone).toInstant()
        return Instant.fromEpochSeconds(from.epochSecond, from.nano) to
            Instant.fromEpochSeconds(until.epochSecond, until.nano)
    }

    private fun toProto(money: Money): MoneyProto =
        MoneyProto
            .newBuilder()
            .setAmountMinor(money.amountMinor)
            .setCurrencyCode(money.currency.name)
            .build()
}
