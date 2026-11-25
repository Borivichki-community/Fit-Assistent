package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams

sealed interface MenuCommand {
    data class GenerateMenu(val params: GenerateBudgetMenuParams) : MenuCommand
    data object CancelGeneration : MenuCommand
}
