package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import kotlin.math.roundToLong

data class ProfileUiModel(
    val id: ProfileId,
    val userId: UserId,
    val displayName: String = "",
    val email: String = "",
    val userRole: UserRole? = null,
    val heightLabel: String,
    val weightLabel: String,
    val genderLabel: String,
    val activityLabel: String,
    val goalLabel: String,
    val caloriesGoalLabel: String,
    val proteinsGoalLabel: String,
    val fatsGoalLabel: String,
    val carbsGoalLabel: String,
    val weightGoalLabel: String,
    val weeklyBudgetLabel: String,
    val dailyBudgetLabel: String = "",
    val weeklyBudgetFormattedLabel: String = "",
    val caloriesGoalFormatted: String = "",
    val proteinsGoalFormatted: String = "",
    val fatsGoalFormatted: String = "",
    val carbsGoalFormatted: String = "",
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
                val resolvedEmail = state.email?.trim().orEmpty()
                ProfileUiState.Content.Success(
                    content.profile.toUiModel(
                        displayName = resolvedName,
                        email = resolvedEmail,
                        userRole = state.userRole,
                    ),
                )
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

    private fun Profile.toUiModel(
        displayName: String,
        email: String,
        userRole: UserRole?,
    ): ProfileUiModel {
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
        val sanitizedWeeklyBudget = if (weeklyBudget.isNaN() || weeklyBudget.isInfinite() || weeklyBudget < 0.0) {
            0.0
        } else {
            weeklyBudget
        }
        val dailyRubles = (sanitizedWeeklyBudget / 7.0).roundToLong()
        val weeklyRubles = sanitizedWeeklyBudget.roundToLong()

        return ProfileUiModel(
            id = id,
            userId = userId,
            displayName = displayName,
            email = email,
            userRole = userRole,
            heightLabel = "$heightCm см",
            weightLabel = "$weightKg кг",
            genderLabel = genderText,
            activityLabel = activityText,
            goalLabel = goalText,
            caloriesGoalLabel = "${nutritionGoal.targetCalories} ккал",
            proteinsGoalLabel = "${nutritionGoal.targetProteinsGrams.toInt()} г",
            fatsGoalLabel = "${nutritionGoal.targetFatsGrams.toInt()} г",
            carbsGoalLabel = "${nutritionGoal.targetCarbsGrams.toInt()} г",
            weightGoalLabel = "${nutritionGoal.targetWeightKg.toInt()} кг",
            weeklyBudgetLabel = "${weeklyBudget.toInt()} ₽ / нед.",
            dailyBudgetLabel = MoneyFormatter.format(Money.fromMinor(dailyRubles * 100L)),
            weeklyBudgetFormattedLabel = MoneyFormatter.format(Money.fromMinor(weeklyRubles * 100L)),
            caloriesGoalFormatted = NutritionFormatter.formatCalories(nutritionGoal.targetCalories.toDouble()),
            proteinsGoalFormatted = NutritionFormatter.formatGram(nutritionGoal.targetProteinsGrams),
            fatsGoalFormatted = NutritionFormatter.formatGram(nutritionGoal.targetFatsGrams),
            carbsGoalFormatted = NutritionFormatter.formatGram(nutritionGoal.targetCarbsGrams),
            isComplete = isComplete,
            rawWeightKg = weightKg,
            rawActivityLevel = activityLevel,
            rawGoalType = goalType,
            rawWeeklyBudget = weeklyBudget,
        )
    }
}
