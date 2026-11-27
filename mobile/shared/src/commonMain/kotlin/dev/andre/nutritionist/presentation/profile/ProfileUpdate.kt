package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.mapper.toProfile
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile

class ProfileUpdate : DslUpdate<ProfileState, ProfileEvent, ProfileCommand, ProfileNews>() {

    override fun NextBuilder<ProfileState, ProfileCommand, ProfileNews>.update(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Ui.LoadProfile -> {
                state {
                    copy(
                        content = ProfileState.Content.Loading,
                        currentUserId = event.userId,
                        editDraft = null,
                        isSaving = false,
                        displayName = event.displayName ?: state.displayName,
                        age = event.age ?: state.age,
                        email = event.email ?: state.email,
                        userRole = event.userRole ?: state.userRole,
                    )
                }
                commands(ProfileCommand.LoadProfile(event.userId))
            }
            ProfileEvent.Ui.Retry -> {
                val draft = state.editDraft
                if (draft != null) {
                    if (!state.isSaving) {
                        state { copy(isSaving = true) }
                        commands(ProfileCommand.UpdateProfile(draft))
                    }
                    return@update
                }
                val userId = state.currentUserId
                if (userId != null) {
                    state { copy(content = ProfileState.Content.Loading) }
                    commands(ProfileCommand.LoadProfile(userId))
                }
            }
            ProfileEvent.Ui.EditClicked -> {
                val userId = state.currentUserId
                if (userId != null) {
                    news(ProfileNews.NavigateToEdit(userId))
                }
            }
            is ProfileEvent.Ui.SaveProfile -> {
                state {
                    copy(
                        content = ProfileState.Content.Form(
                            name = event.name,
                            age = event.age,
                            targetWeight = event.targetWeight,
                            dailyCalories = event.dailyCalories,
                        ),
                    )
                }
            }
            is ProfileEvent.Ui.SaveOnboardingProfile -> {
                val profile = event.data.toProfile(event.userId)
                val displayName = listOf(event.data.firstName, event.data.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() }
                state {
                    copy(
                        content = ProfileState.Content.Loading,
                        currentUserId = event.userId,
                        isSaving = true,
                        displayName = displayName ?: state.displayName,
                        age = event.data.age.takeIf { it.isNotBlank() } ?: state.age,
                    )
                }
                commands(ProfileCommand.UpdateProfile(profile))
            }
            is ProfileEvent.Ui.UpdateProfile -> {
                state { copy(content = ProfileState.Content.Loading, isSaving = true) }
                commands(ProfileCommand.UpdateProfile(event.profile))
            }
            is ProfileEvent.Ui.ChangeWeight -> {
                val currentContent = state.content
                if (currentContent is ProfileState.Content.Success && !state.isSaving) {
                    val updatedProfile = currentContent.profile.copy(
                        weightKg = event.weightKg,
                        nutritionGoal = NutritionGoal.EMPTY,
                    )
                    state { copy(isSaving = true, editDraft = updatedProfile) }
                    commands(ProfileCommand.UpdateProfile(updatedProfile))
                }
            }
            is ProfileEvent.Ui.ChangeActivity -> {
                val currentContent = state.content
                if (currentContent is ProfileState.Content.Success && !state.isSaving) {
                    val updatedProfile = currentContent.profile.copy(
                        activityLevel = event.activityLevel,
                        nutritionGoal = NutritionGoal.EMPTY,
                    )
                    state { copy(isSaving = true, editDraft = updatedProfile) }
                    commands(ProfileCommand.UpdateProfile(updatedProfile))
                }
            }
            is ProfileEvent.Ui.ChangeGoal -> {
                val currentContent = state.content
                if (currentContent is ProfileState.Content.Success && !state.isSaving) {
                    val updatedProfile = currentContent.profile.copy(
                        goalType = event.goalType,
                        nutritionGoal = NutritionGoal.EMPTY,
                    )
                    state { copy(isSaving = true, editDraft = updatedProfile) }
                    commands(ProfileCommand.UpdateProfile(updatedProfile))
                }
            }
            is ProfileEvent.Ui.ChangeBudget -> {
                val currentContent = state.content
                if (currentContent is ProfileState.Content.Success && !state.isSaving) {
                    val updatedProfile = currentContent.profile.copy(weeklyBudget = event.weeklyBudget)
                    state { copy(isSaving = true, editDraft = updatedProfile) }
                    commands(ProfileCommand.UpdateProfile(updatedProfile))
                }
            }
            is ProfileEvent.Ui.NameChanged -> {
                state {
                    when (val c = content) {
                        is ProfileState.Content.Form -> copy(content = c.copy(name = event.name))
                        is ProfileState.Content.ValidationError -> copy(content = c.copy(name = event.name))
                        ProfileState.Content.Loading -> copy(content = ProfileState.Content.Form(name = event.name))
                        is ProfileState.Content.Error -> copy(content = ProfileState.Content.Form(name = event.name))
                        is ProfileState.Content.Success -> copy(content = ProfileState.Content.Form(name = event.name))
                    }
                }
            }
            is ProfileEvent.Ui.AgeChanged -> {
                state {
                    when (val c = content) {
                        is ProfileState.Content.Form -> copy(content = c.copy(age = event.age))
                        is ProfileState.Content.ValidationError -> copy(content = c.copy(age = event.age))
                        ProfileState.Content.Loading -> copy(content = ProfileState.Content.Form(age = event.age))
                        is ProfileState.Content.Error -> copy(content = ProfileState.Content.Form(age = event.age))
                        is ProfileState.Content.Success -> copy(content = ProfileState.Content.Form(age = event.age))
                    }
                }
            }
            is ProfileEvent.Ui.TargetWeightChanged -> {
                state {
                    when (val c = content) {
                        is ProfileState.Content.Form -> copy(content = c.copy(targetWeight = event.targetWeight))
                        is ProfileState.Content.ValidationError -> copy(content = c.copy(targetWeight = event.targetWeight))
                        ProfileState.Content.Loading -> copy(content = ProfileState.Content.Form(targetWeight = event.targetWeight))
                        is ProfileState.Content.Error -> copy(content = ProfileState.Content.Form(targetWeight = event.targetWeight))
                        is ProfileState.Content.Success -> copy(content = ProfileState.Content.Form(targetWeight = event.targetWeight))
                    }
                }
            }
            is ProfileEvent.Ui.DailyCaloriesChanged -> {
                state {
                    when (val c = content) {
                        is ProfileState.Content.Form -> copy(content = c.copy(dailyCalories = event.dailyCalories))
                        is ProfileState.Content.ValidationError -> copy(content = c.copy(dailyCalories = event.dailyCalories))
                        ProfileState.Content.Loading -> copy(content = ProfileState.Content.Form(dailyCalories = event.dailyCalories))
                        is ProfileState.Content.Error -> copy(content = ProfileState.Content.Form(dailyCalories = event.dailyCalories))
                        is ProfileState.Content.Success -> copy(content = ProfileState.Content.Form(dailyCalories = event.dailyCalories))
                    }
                }
            }
            is ProfileEvent.Internal.ProfileLoaded -> {
                val wasSaving = state.isSaving
                val currentSuccess = state.content as? ProfileState.Content.Success
                val resolvedDisplayName = state.displayName ?: currentSuccess?.displayName
                val resolvedAge = state.age ?: currentSuccess?.age
                state {
                    copy(
                        content = ProfileState.Content.Success(
                            profile = event.profile,
                            displayName = resolvedDisplayName,
                            age = resolvedAge,
                        ),
                        isSaving = false,
                        editDraft = null,
                        displayName = resolvedDisplayName,
                        age = resolvedAge,
                    )
                }
                if (wasSaving) {
                    news(ProfileNews.ProfileSaved)
                }
            }
            is ProfileEvent.Internal.ProfileLoadFailed -> {
                if (state.isSaving && state.content is ProfileState.Content.Success) {
                    state { copy(isSaving = false) }
                    news(ProfileNews.ShowSnackbar("Не удалось сохранить профиль. Проверьте подключение и повторите попытку."))
                    return@update
                }
                val errorMessage = event.error.message?.takeIf { it.isNotBlank() } ?: "Не удалось загрузить профиль"
                state { copy(content = ProfileState.Content.Error(errorMessage), isSaving = false) }
                news(ProfileNews.ShowSnackbar(errorMessage))
            }
        }
    }
}
