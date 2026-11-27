package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dev.andre.nutritionist.domain.model.NutritionFacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.airecipe.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AiRecipeRoute(mealType: MealType, dateEpochMs: Long, targetCalories: Int, onBackClick: () -> Unit, onRecipeAdded: () -> Unit, remainingNutrition: NutritionFacts) {
    val vm: AiRecipeViewModel = koinViewModel { parametersOf(AiRecipeState(AiRecipePrompt(mealType, targetCalories, remainingNutrition = remainingNutrition),dateEpochMs)) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    BackHandler { vm.dispatch(AiRecipeEvent.Ui.BackClicked) }
    LaunchedEffect(vm) {
        vm.news.collect {
            when (it) {
                AiRecipeNews.Dismiss -> onBackClick()
                AiRecipeNews.RecipeAddedSuccess -> onRecipeAdded()
                is AiRecipeNews.ShowSnackbar -> snackbar.showSnackbar(it.message)
            }
        }
    }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        AiRecipeScreen(state,vm::dispatch)
        SnackbarHost(snackbar, androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}
