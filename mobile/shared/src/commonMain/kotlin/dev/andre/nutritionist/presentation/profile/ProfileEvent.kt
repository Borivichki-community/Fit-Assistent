package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId

sealed interface ProfileEvent {

    /** События, инициированные пользователем через UI. */
    sealed interface Ui : ProfileEvent {
        data class LoadProfile(val userId: UserId) : Ui
        data object Retry : Ui
        data object EditClicked : Ui
    }

    /** Внутренние события, приходящие из CommandsFlowHandler. */
    sealed interface Internal : ProfileEvent {
        data class ProfileLoaded(val profile: Profile) : Internal
        data class ProfileLoadFailed(val error: Throwable) : Internal
    }
}