package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.recipe.v1.GenerateMealPlanRequest
import com.budgetntr.proto.recipe.v1.GenerateMealPlanResponse
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuRequest
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuResponse
import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
import dev.andre.nutritionist.data.mapper.RecipeDtoMapper.toDomain
import dev.andre.nutritionist.data.mapper.RecipeDtoMapper.toRequest
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

interface RecipeGrpcServiceClient {
    suspend fun generateMealPlan(request: GenerateMealPlanRequest): GenerateMealPlanResponse
    fun generateWeekMenu(request: GenerateWeekMenuRequest): Flow<GenerateWeekMenuResponse>
}

class RecipeGrpcRemoteDataSource(
    private val client: RecipeGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
) : RecipeRemoteDataSource {

    override suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan> =
        withContext(dispatchers.io) {
            runCatching {
                val request = params.toRequest()
                val response = client.generateMealPlan(request)
                response.toDomain()
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
        }

    override fun generateWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String>,
        preferredProductIds: List<String>,
    ): Flow<WeekMenuDayProto> {
        val request = GenerateWeekMenuRequest(
            dailyCalories = dailyCalories,
            weeklyBudgetMinor = weeklyBudgetMinor,
            excludedProductIds = excludedProductIds,
            preferredProductIds = preferredProductIds,
        )
        return client.generateWeekMenu(request)
            .mapNotNull { it.dayReady }
            .flowOn(dispatchers.io)
    }
}
