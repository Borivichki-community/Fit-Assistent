package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.core.kotea.DslUpdate

class ProfileUpdate : DslUpdate<ProfileState, ProfileEvent, ProfileCommand, ProfileNews>() {

    override fun NextBuilder<ProfileState, ProfileCommand, ProfileNews>.update(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Ui.LoadProfile -> {
                state { copy(content = ProfileState.Content.Loading, currentUserId = event.userId) }
                commands(ProfileCommand.LoadProfile(event.userId))
            }
            ProfileEvent.Ui.Retry -> {
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
                        )
                    )
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
                state { copy(content = ProfileState.Content.Success(event.profile)) }
            }
            is ProfileEvent.Internal.ProfileLoadFailed -> {
                val errorMessage = event.error.message?.takeIf { it.isNotBlank() } ?: "Не удалось загрузить профиль"
                state { copy(content = ProfileState.Content.Error(errorMessage)) }
                news(ProfileNews.ShowSnackbar(errorMessage))
            }
        }
    }
}
