package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanItem
import dev.andre.nutritionist.domain.model.MealPlanItemId
import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.PlanItemType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeAuthorType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.repository.CoachingRepository
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import dev.andre.nutritionist.domain.repository.RecipeCatalogRepository
import dev.andre.nutritionist.domain.usecase.ApplyMealPlanDayToDiaryUseCase
import dev.andre.nutritionist.domain.usecase.LogRecipeToDiaryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MealPlansCommandsFlowHandler(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeCatalogRepository: RecipeCatalogRepository,
    private val coachingRepository: CoachingRepository,
    private val logRecipeToDiaryUseCase: LogRecipeToDiaryUseCase,
    private val applyMealPlanDayToDiaryUseCase: ApplyMealPlanDayToDiaryUseCase,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<MealPlansCommand, MealPlansEvent> {

    override fun handle(commands: Flow<MealPlansCommand>): Flow<MealPlansEvent> = flow {
        commands.collect { command ->
            when (command) {
                is MealPlansCommand.LoadAll -> {
                    val event = try {
                        coroutineScope {
                            val plansDeferred = async(dispatchers.io) {
                                mealPlanRepository.searchMealPlans(
                                    MealPlanSearchCriteria(query = command.query, pageSize = 30),
                                )
                            }
                            val publicRecipesDeferred = async(dispatchers.io) {
                                recipeCatalogRepository.searchPublicRecipes(
                                    RecipeSearchCriteria(query = command.query, pageSize = 30),
                                )
                            }
                            val localRecipesDeferred = async(dispatchers.io) {
                                recipeCatalogRepository.observeMyRecipes().firstOrNull().orEmpty()
                            }
                            val selectedDeferred = command.initialPlanId?.let { id ->
                                async(dispatchers.io) { mealPlanRepository.getMealPlanById(id) }
                            }

                            val plansResult = plansDeferred.await()
                            val recipesResult = publicRecipesDeferred.await()
                            val localRecipes = localRecipesDeferred.await()
                            val selectedPlan = selectedDeferred?.await()?.getOrNull()

                            if (plansResult.isFailure && recipesResult.isFailure && localRecipes.isEmpty()) {
                                val err = plansResult.exceptionOrNull()
                                    ?: recipesResult.exceptionOrNull()
                                    ?: IllegalStateException("Ошибка загрузки планов")
                                if (err is CancellationException) throw err
                                MealPlansEvent.Internal.LoadFailed(err)
                            } else {
                                val mergedRecipes = (recipesResult.getOrNull()?.items.orEmpty() + localRecipes)
                                    .distinctBy { it.id }
                                MealPlansEvent.Internal.DataLoaded(
                                    plans = plansResult.getOrNull()?.items.orEmpty(),
                                    selectedPlan = selectedPlan,
                                    recipes = mergedRecipes,
                                )
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        MealPlansEvent.Internal.LoadFailed(error)
                    }
                    emit(event)
                }

                is MealPlansCommand.CreatePlan -> {
                    val planId = MealPlanTemplateId(Uuid.random().toString())
                    val dayItems = buildStarterDays(planId, command.durationDays, command.starterRecipe)
                    val plan = MealPlanTemplate(
                        id = planId,
                        authorId = command.authorId,
                        authorType = if (command.isCoach) RecipeAuthorType.COACH else RecipeAuthorType.USER,
                        title = command.title,
                        description = command.description.ifBlank { null },
                        durationDays = command.durationDays,
                        visibility = command.visibility,
                        accessMode = command.accessMode,
                        price = Money.fromMinor(command.priceMinor),
                        days = dayItems,
                    )
                    val result = withContext(dispatchers.io) {
                        mealPlanRepository.saveMealPlan(plan)
                    }
                    result.fold(
                        onSuccess = { saved ->
                            emit(MealPlansEvent.Internal.PlanSaved(saved, "Меню сохранено (${saved.title})"))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.PublishPlan -> {
                    val updated = command.plan.copy(visibility = ContentVisibility.PUBLIC)
                    val result = withContext(dispatchers.io) {
                        mealPlanRepository.saveMealPlan(updated)
                    }
                    result.fold(
                        onSuccess = { saved ->
                            emit(MealPlansEvent.Internal.PlanSaved(saved, "Меню опубликовано в общий доступ"))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.LoadPlanDetails -> {
                    val result = withContext(dispatchers.io) {
                        mealPlanRepository.getMealPlanById(command.planId)
                    }
                    result.fold(
                        onSuccess = { plan -> emit(MealPlansEvent.Internal.PlanDetailsLoaded(plan)) },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.SubscribeToPlan -> {
                    val result = withContext(dispatchers.io) {
                        coachingRepository.grantAccess(
                            userId = command.userId,
                            targetType = GrantTargetType.MEAL_PLAN,
                            targetId = command.planId.raw,
                            grantSource = GrantSource.SUBSCRIPTION,
                        )
                    }
                    result.fold(
                        onSuccess = {
                            emit(MealPlansEvent.Internal.ActionSucceeded("Подписка на меню оформлена"))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.ApplyPlanDay -> {
                    val result = applyMealPlanDayToDiaryUseCase(
                        plan = command.plan,
                        dayNumber = command.dayNumber,
                    )
                    result.fold(
                        onSuccess = { records ->
                            emit(
                                MealPlansEvent.Internal.ActionSucceeded(
                                    "В дневник добавлено блюд: ${records.size}",
                                ),
                            )
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.PublishRecipe -> {
                    val result = withContext(dispatchers.io) {
                        recipeCatalogRepository.publishRecipe(command.recipeId, ContentVisibility.PUBLIC)
                    }
                    result.fold(
                        onSuccess = { recipe ->
                            emit(MealPlansEvent.Internal.RecipeUpdated(recipe, "Рецепт опубликован (PUBLIC)"))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.ForkRecipe -> {
                    val result = withContext(dispatchers.io) {
                        recipeCatalogRepository.forkRecipe(originId = command.recipeId)
                    }
                    result.fold(
                        onSuccess = { recipe ->
                            emit(MealPlansEvent.Internal.RecipeUpdated(recipe, "Скопировано к себе: ${recipe.title}"))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.LogRecipeToDiary -> {
                    val weight = command.recipe.totalWeightGrams.takeIf { it > 0.0 }
                        ?: command.recipe.ingredients.sumOf { it.weightGrams }.takeIf { it > 0.0 }
                        ?: 200.0
                    val portionWeight = weight / command.recipe.servingsCount.coerceAtLeast(1)
                    val result = logRecipeToDiaryUseCase(
                        recipe = command.recipe,
                        weightGrams = portionWeight,
                    )
                    result.fold(
                        onSuccess = { record ->
                            emit(
                                MealPlansEvent.Internal.ActionSucceeded(
                                    "Записано в дневник: ${record.nameSnapshot}",
                                ),
                            )
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }

                is MealPlansCommand.CreateQuickRecipe -> {
                    val recipe = Recipe(
                        id = RecipeId(Uuid.random().toString()),
                        title = "Белковый боул с киноа",
                        authorType = if (command.isCoach) RecipeAuthorType.COACH else RecipeAuthorType.USER,
                        authorId = command.authorId,
                        description = "Быстрый сбалансированный приём пищи",
                        visibility = ContentVisibility.PRIVATE,
                        servingsCount = 1,
                        totalWeightGrams = 300.0,
                        nutrition = NutritionFacts(calories = 160.0, proteins = 14.0, fats = 5.0, carbs = 16.0),
                        mealType = MealType.LUNCH,
                        estimatedCost = Money.fromMinor(24_000L),
                        cookingTimeMinutes = 15,
                    )
                    val result = withContext(dispatchers.io) {
                        recipeCatalogRepository.saveRecipe(recipe)
                    }
                    result.fold(
                        onSuccess = { saved ->
                            emit(MealPlansEvent.Internal.RecipeUpdated(saved, "Создан рецепт: ${saved.title}"))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(MealPlansEvent.Internal.ActionFailed(error))
                        },
                    )
                }
            }
        }
    }

    private fun buildStarterDays(
        planId: MealPlanTemplateId,
        durationDays: Int,
        starterRecipe: Recipe?,
    ): List<MealPlanDay> {
        val recipe = starterRecipe ?: return emptyList()
        val weight = recipe.totalWeightGrams.takeIf { it > 0.0 } ?: 250.0
        val portionNutrition = recipe.nutrition * (weight / 100.0)
        return (1..durationDays).map { day ->
            MealPlanDay(
                dayNumber = day,
                items = listOf(
                    MealPlanItem(
                        id = MealPlanItemId(Uuid.random().toString()),
                        planId = planId,
                        dayNumber = day,
                        mealSlot = recipe.mealType,
                        itemType = PlanItemType.RECIPE,
                        itemId = recipe.id.raw,
                        nameSnapshot = recipe.title,
                        weightGrams = weight,
                        nutrition = portionNutrition,
                        estimatedCost = recipe.estimatedCost,
                    ),
                ),
                totalNutrition = portionNutrition,
                estimatedCost = recipe.estimatedCost,
            )
        }
    }
}
