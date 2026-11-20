package dev.andre.nutritionist.domain.mapper

import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.OnboardingData
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId

/**
 * Чистый маппер для преобразования данных сессии онбординга [OnboardingData]
 * в доменную сущность профиля [Profile].
 *
 * Расчёт целевых норм КБЖУ делегирован бэкенду (profile-service, SSOT).
 * Клиент передаёт дефолтный пустой [NutritionGoal], который сервер автоматически
 * рассчитывает по формуле Миффлина — Сан-Жеора и возвращает в ответе.
 */
object OnboardingDataMapper {

    private const val DEFAULT_HEIGHT_CM = 170
    private const val DEFAULT_WEIGHT_KG = 70.0
    private const val DEFAULT_WEEKLY_BUDGET = 5000.0

    fun toProfile(
        data: OnboardingData,
        userId: UserId,
        profileId: ProfileId = ProfileId("p-${userId.raw}"),
        updatedAt: kotlin.time.Instant = kotlin.time.Instant.fromEpochMilliseconds(0L),
    ): Profile {
        val height = data.heightCm.toIntOrNull() ?: DEFAULT_HEIGHT_CM
        val weight = data.weightKg.toDoubleOrNull() ?: DEFAULT_WEIGHT_KG
        val budget = data.weeklyBudget.toDoubleOrNull() ?: DEFAULT_WEEKLY_BUDGET

        return Profile(
            id = profileId,
            userId = userId,
            heightCm = height,
            weightKg = weight,
            gender = data.gender,
            activityLevel = data.activityLevel,
            goalType = data.goalType,
            nutritionGoal = NutritionGoal.EMPTY,
            weeklyBudget = budget,
            updatedAt = updatedAt,
        )
    }
}

fun OnboardingData.toProfile(
    userId: UserId,
    profileId: ProfileId = ProfileId("p-${userId.raw}"),
    updatedAt: kotlin.time.Instant = kotlin.time.Instant.fromEpochMilliseconds(0L),
): Profile = OnboardingDataMapper.toProfile(
    data = this,
    userId = userId,
    profileId = profileId,
    updatedAt = updatedAt,
)
