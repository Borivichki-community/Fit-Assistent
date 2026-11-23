package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId

data class ProfileUiModel(
    val id: ProfileId,
    val userId: UserId,
    val displayName: String = "",
    val email: String = "john.athlete@fitapp.io",
    val isPro: Boolean = true,
    val streakDaysText: String = "14 дней подряд",
    val streakSubtitleText: String = "Дисциплина в норме",
    val streakLevelText: String = "Уровень 4",
    val weightChangeText: String = "↓ 1.4 кг за месяц",
    val heightLabel: String,
    val weightLabel: String,
    val genderLabel: String,
    val activityLabel: String,
    val activitySubtitle: String = "1–3 тренировки / нед",
    val goalLabel: String,
    val targetWeightSubtitle: String = "Целевой вес: 65 кг",
    val caloriesGoalLabel: String,
    val proteinsGoalLabel: String,
    val fatsGoalLabel: String,
    val carbsGoalLabel: String,
    val weightGoalLabel: String,
    val weeklyBudgetLabel: String,
    val dailyBudgetText: String = "500 ₽ / день",
    val weeklyBudgetText: String = "~3 500 ₽ в неделю",
    val planSummaryText: String = "Сушка 2400 ккал • Кето баланс",
    val isNotificationsEnabled: Boolean = true,
    val versionText: String = "FitApp v2.4.1 (Build 412)",
    val isComplete: Boolean = true,
    val rawWeightKg: Double = 0.0,
    val rawActivityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val rawGoalType: GoalType = GoalType.LOSE_WEIGHT,
    val rawWeeklyBudget: Double = 0.0,
)

data class ProfileUiState(
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Form(
            val name: String = "",
            val age: String = "",
            val targetWeight: String = "",
            val dailyCalories: String = "",
        ) : Content
        data class Success(val profile: ProfileUiModel) : Content
        data class Error(val message: String) : Content
        data class ValidationError(
            val name: String = "",
            val age: String = "",
            val targetWeight: String = "",
            val dailyCalories: String = "",
            val nameError: String? = null,
            val ageError: String? = null,
            val targetWeightError: String? = null,
            val dailyCaloriesError: String? = null,
        ) : Content
    }
}

class ProfileUiStateMapper : UiStateMapper<ProfileState, ProfileUiState> {

    override fun map(state: ProfileState): ProfileUiState {
        val uiContent = when (val content = state.content) {
            ProfileState.Content.Loading -> ProfileUiState.Content.Loading
            is ProfileState.Content.Form -> ProfileUiState.Content.Form(
                name = content.name,
                age = content.age,
                targetWeight = content.targetWeight,
                dailyCalories = content.dailyCalories,
            )
            is ProfileState.Content.Success -> {
                val resolvedName = (content.displayName ?: state.displayName)?.takeIf { it.isNotBlank() }
                    ?: content.profile.userId.raw
                ProfileUiState.Content.Success(content.profile.toUiModel(resolvedName))
            }
            is ProfileState.Content.Error -> ProfileUiState.Content.Error(content.message)
            is ProfileState.Content.ValidationError -> ProfileUiState.Content.ValidationError(
                name = content.name,
                age = content.age,
                targetWeight = content.targetWeight,
                dailyCalories = content.dailyCalories,
                nameError = content.nameError,
                ageError = content.ageError,
                targetWeightError = content.targetWeightError,
                dailyCaloriesError = content.dailyCaloriesError,
            )
        }
        return ProfileUiState(content = uiContent)
    }

    private fun Profile.toUiModel(displayName: String): ProfileUiModel {
        val genderText = when (gender) {
            Gender.MALE -> "Мужской"
            Gender.FEMALE -> "Женский"
            Gender.UNKNOWN -> "Не указан"
        }

        val activityText = when (activityLevel) {
            ActivityLevel.SEDENTARY -> "Сидячий образ жизни"
            ActivityLevel.LIGHT -> "Лёгкая активность"
            ActivityLevel.MODERATE -> "Умеренная активность"
            ActivityLevel.VERY_ACTIVE -> "Высокая активность"
            ActivityLevel.UNKNOWN -> "Не указано"
        }

        val goalText = when (goalType) {
            GoalType.LOSE_WEIGHT -> "Снижение веса"
            GoalType.MAINTAIN -> "Поддержание веса"
            GoalType.GAIN_WEIGHT -> "Набор массы"
            GoalType.UNKNOWN -> "Не указано"
        }

        val isComplete = heightCm > 0 && weightKg > 0.0

        val dailyBudgetVal = if (weeklyBudget > 0) (weeklyBudget / 7.0).toInt() else 500
        val dailyBudgetFormatted = "$dailyBudgetVal ₽ / день"
        val weeklyBudgetFormatted = if (weeklyBudget > 0) "~${weeklyBudget.toInt()} ₽ в неделю" else "~3 500 ₽ в неделю"

        val activitySub = when (activityLevel) {
            ActivityLevel.SEDENTARY -> "Минимум нагрузок"
            ActivityLevel.LIGHT -> "1–3 тренировки / нед"
            ActivityLevel.MODERATE -> "3–5 тренировок / нед"
            ActivityLevel.VERY_ACTIVE -> "6–7 тренировок / нед"
            ActivityLevel.UNKNOWN -> "1–3 тренировки / нед"
        }

        val targetWeightSub = if (nutritionGoal.targetWeightKg > 0) {
            "Целевой вес: ${nutritionGoal.targetWeightKg.toInt()} кг"
        } else {
            "Целевой вес: 65 кг"
        }

        val targetCaloriesVal = if (nutritionGoal.targetCalories > 0) nutritionGoal.targetCalories else 2400
        val planSummary = "Сушка $targetCaloriesVal ккал • Кето баланс"
        val resolvedEmail = if (displayName.contains("@")) displayName else "john.athlete@fitapp.io"

        return ProfileUiModel(
            id = id,
            userId = userId,
            displayName = displayName,
            email = resolvedEmail,
            isPro = true,
            streakDaysText = "14 дней подряд",
            streakSubtitleText = "Дисциплина в норме",
            streakLevelText = "Уровень 4",
            weightChangeText = "↓ 1.4 кг за месяц",
            heightLabel = "$heightCm см",
            weightLabel = "$weightKg кг",
            genderLabel = genderText,
            activityLabel = activityText,
            activitySubtitle = activitySub,
            goalLabel = goalText,
            targetWeightSubtitle = targetWeightSub,
            caloriesGoalLabel = "${nutritionGoal.targetCalories} ккал",
            proteinsGoalLabel = "${nutritionGoal.targetProteinsGrams.toInt()} г",
            fatsGoalLabel = "${nutritionGoal.targetFatsGrams.toInt()} г",
            carbsGoalLabel = "${nutritionGoal.targetCarbsGrams.toInt()} г",
            weightGoalLabel = "${nutritionGoal.targetWeightKg.toInt()} кг",
            weeklyBudgetLabel = "${weeklyBudget.toInt()} ₽ / нед.",
            dailyBudgetText = dailyBudgetFormatted,
            weeklyBudgetText = weeklyBudgetFormatted,
            planSummaryText = planSummary,
            isNotificationsEnabled = true,
            versionText = "FitApp v2.4.1 (Build 412)",
            isComplete = isComplete,
            rawWeightKg = weightKg,
            rawActivityLevel = activityLevel,
            rawGoalType = goalType,
            rawWeeklyBudget = weeklyBudget,
        )
    }
}
