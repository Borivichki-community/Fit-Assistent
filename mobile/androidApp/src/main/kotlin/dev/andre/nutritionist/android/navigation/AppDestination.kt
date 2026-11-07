package dev.andre.nutritionist.android.navigation

sealed class AppDestination(val route: String) {
    data object Auth : AppDestination("auth")
    data object SignUp : AppDestination("sign_up")
    data object Registration : AppDestination("registration")
    data object Dashboard : AppDestination("dashboard")
    data object Diary : AppDestination("diary")
    data object Recipes : AppDestination("recipes")
    data object Statistics : AppDestination("statistics")
    data object Profile : AppDestination("profile")
}
