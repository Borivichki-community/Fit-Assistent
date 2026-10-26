package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.UserId

sealed interface ProfileCommand {
    data class LoadProfile(val userId: UserId) : ProfileCommand
}