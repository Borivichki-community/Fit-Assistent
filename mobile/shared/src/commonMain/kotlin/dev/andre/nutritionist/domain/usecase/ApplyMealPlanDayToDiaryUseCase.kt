package dev.andre.nutritionist.domain.usecase

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanItem
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.PlanItemType
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.DiaryRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

class ApplyMealPlanDayToDiaryUseCase(
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers(),
) {

    suspend operator fun invoke(
        day: MealPlanDay,
        recordedAt: Instant = Clock.System.now(),
    ): Result<List<MealRecord>> = withContext(dispatchers.io) {
        if (day.items.isEmpty()) {
            return@withContext Result.failure(
                DomainError.ValidationError(field = "items", reason = "Meal plan day must contain at least one item"),
            )
        }

        val invalidItem = day.items.firstOrNull {
            it.weightGrams <= 0.0 || it.weightGrams.isNaN() || it.weightGrams.isInfinite()
        }
        if (invalidItem != null) {
            return@withContext Result.failure(
                DomainError.ValidationError(
                    field = "weightGrams",
                    reason = "Item '${invalidItem.nameSnapshot}' must have positive weight",
                ),
            )
        }

        val savedRecords = ArrayList<MealRecord>(day.items.size)
        try {
            for (item in day.items) {
                val record = item.toMealRecord(recordedAt)
                val addResult = diaryRepository.addEntry(record)
                val saved = addResult.getOrElse { error ->
                    return@withContext Result.failure(error)
                }
                savedRecords.add(saved)
            }
            Result.success(savedRecords)
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend operator fun invoke(
        plan: MealPlanTemplate,
        dayNumber: Int,
        recordedAt: Instant = Clock.System.now(),
    ): Result<List<MealRecord>> {
        val day = plan.days.firstOrNull { it.dayNumber == dayNumber }
            ?: return Result.failure(
                DomainError.NotFound(entity = "MealPlanDay", id = dayNumber.toString()),
            )
        return invoke(day = day, recordedAt = recordedAt)
    }

    private fun MealPlanItem.toMealRecord(recordedAt: Instant): MealRecord = MealRecord(
        id = MealRecordId.generate(),
        mealType = mealSlot,
        recordedAt = recordedAt,
        sourceType = when (itemType) {
            PlanItemType.RECIPE -> EntrySourceType.RECIPE
            PlanItemType.PRODUCT -> EntrySourceType.PRODUCT
            PlanItemType.UNSPECIFIED -> EntrySourceType.CUSTOM
        },
        sourceId = itemId.ifBlank { null },
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        nutrition = nutrition,
        cost = estimatedCost,
        idempotencyKey = IdempotencyKey.generate(),
        syncStatus = SyncStatus.PENDING,
    )
}
