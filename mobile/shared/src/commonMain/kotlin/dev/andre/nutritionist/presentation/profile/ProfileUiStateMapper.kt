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
            is ProfileState.Content.Success -> ProfileUiState.Content.Success(content.profile.toUiModel())
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

    private fun Profile.toUiModel(): ProfileUiModel {
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

        return ProfileUiModel(
            id = id,
            userId = userId,
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
        )
    }
}
