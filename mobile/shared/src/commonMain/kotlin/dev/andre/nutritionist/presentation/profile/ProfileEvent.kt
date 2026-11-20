package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.OnboardingData
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId

sealed interface ProfileEvent {

    /** События, инициированные пользователем через UI. */
    sealed interface Ui : ProfileEvent {
        data class LoadProfile(
            val userId: UserId,
            val displayName: String? = null,
            val age: String? = null,
        ) : Ui

        data object Retry : Ui
        data object EditClicked : Ui

        data class SaveProfile(
            val name: String,
            val age: String,
            val targetWeight: String,
            val dailyCalories: String,
        ) : Ui

        data class SaveOnboardingProfile(
            val data: OnboardingData,
            val userId: UserId,
        ) : Ui

        data class UpdateProfile(val profile: Profile) : Ui

        data class ChangeWeight(val weightKg: Double) : Ui
        data class ChangeActivity(val activityLevel: ActivityLevel) : Ui
        data class ChangeGoal(val goalType: GoalType) : Ui
        data class ChangeBudget(val weeklyBudget: Double) : Ui

        data class NameChanged(val name: String) : Ui
        data class AgeChanged(val age: String) : Ui
        data class TargetWeightChanged(val targetWeight: String) : Ui
        data class DailyCaloriesChanged(val dailyCalories: String) : Ui
    }

    /** Внутренние события, приходящие из CommandsFlowHandler. */
    sealed interface Internal : ProfileEvent {
        data class ProfileLoaded(val profile: Profile) : Internal
        data class ProfileLoadFailed(val error: Throwable) : Internal
    }
}
