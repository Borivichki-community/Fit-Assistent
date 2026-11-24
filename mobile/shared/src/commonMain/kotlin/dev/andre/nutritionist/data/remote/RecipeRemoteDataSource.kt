package dev.andre.nutritionist.data.remote

import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import kotlinx.coroutines.flow.Flow

interface RecipeRemoteDataSource {
    suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan>
    fun generateWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String> = emptyList(),
        preferredProductIds: List<String> = emptyList(),
    ): Flow<WeekMenuDayProto>
}
