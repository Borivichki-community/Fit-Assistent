package dev.andre.nutritionist.android.navigation

sealed class AppDestination(val route: String) {
    data object Auth : AppDestination("auth")
    data object SignUp : AppDestination("sign_up")
    data object RoleSelection : AppDestination("role_selection")
    data object Registration : AppDestination("registration")
    data class AiRecipe(val mealType: dev.andre.nutritionist.domain.model.MealType, val dateEpochMs: Long, val targetCalories: Int) :
        AppDestination("ai-recipe/$mealType/$dateEpochMs/$targetCalories") {
        companion object { const val pattern = "ai-recipe/{slot}/{date}/{calories}" }
    }
    data object Dashboard : AppDestination("dashboard")
    data object Diary : AppDestination("diary")
    data object Recipes : AppDestination("recipes")
    data object Statistics : AppDestination("statistics")
    data object Profile : AppDestination("profile")
}
