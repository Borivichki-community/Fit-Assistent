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