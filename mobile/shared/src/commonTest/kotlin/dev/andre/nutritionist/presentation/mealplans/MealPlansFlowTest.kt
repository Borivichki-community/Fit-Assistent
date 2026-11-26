package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.usecase.ApplyMealPlanDayToDiaryUseCase
import dev.andre.nutritionist.domain.usecase.LogRecipeToDiaryUseCase
import dev.andre.nutritionist.fake.FakeCoachingRepository
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.FakeMealPlanRepository
import dev.andre.nutritionist.fake.FakeRecipeCatalogRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MealPlansFlowTest {

    @Test
    fun `update restricts non-coach from PAID mode and builds CreatePlan command`() {
        val update = MealPlansUpdate()
        val clientState = MealPlansState(currentUserId = UserId("u1"), isCoach = false)
        val afterPaidAttempt = requireNotNull(
            update.update(clientState, MealPlansEvent.Ui.AccessModeSelected(AccessMode.PAID)).state,
        )
        assertEquals(AccessMode.FREE, afterPaidAttempt.accessMode)

        val coachState = MealPlansState(
            currentUserId = UserId("coach-1"),
            isCoach = true,
            titleInput = "Сушка 7 дней",
            durationDays = 7,
            accessMode = AccessMode.PAID,
            priceRubInput = "1200",
        )
        val cmd = assertIs<MealPlansCommand.CreatePlan>(
            update.update(coachState, MealPlansEvent.Ui.CreatePlanClicked).commands.single(),
        )
        assertEquals(7, cmd.durationDays)
        assertEquals(120_000L, cmd.priceMinor)
        assertEquals(AccessMode.PAID, cmd.accessMode)
    }

    @Test
    fun `handler creates publishes subscribes and logs recipe or plan day to diary`() = runTest {
        val dispatchers = TestCoroutineDispatchers()
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeCatalogRepository()
        val coachingRepo = FakeCoachingRepository()
        val diaryRepo = FakeDiaryRepository()
        val starterRecipe = Recipe(
            id = RecipeId("r-1"),
            title = "Курица с гречкой",
            totalWeightGrams = 300.0,
            nutrition = NutritionFacts(150.0, 18.0, 4.0, 12.0),
            mealType = MealType.LUNCH,
            estimatedCost = Money.fromMinor(21_000L),
        )
        recipeRepo.saveRecipe(starterRecipe)

        val handler = MealPlansCommandsFlowHandler(
            mealPlanRepository = planRepo,
            recipeCatalogRepository = recipeRepo,
            coachingRepository = coachingRepo,
            logRecipeToDiaryUseCase = LogRecipeToDiaryUseCase(diaryRepo, dispatchers),
            applyMealPlanDayToDiaryUseCase = ApplyMealPlanDayToDiaryUseCase(diaryRepo, dispatchers),
            dispatchers = dispatchers,
        )

        val created = assertIs<MealPlansEvent.Internal.PlanSaved>(
            handler.handle(
                flowOf(
                    MealPlansCommand.CreatePlan(
                        authorId = UserId("coach-1"),
                        isCoach = true,
                        title = "Неделя ПП",
                        description = "Описание",
                        durationDays = 7,
                        accessMode = AccessMode.FREE,
                        priceMinor = 0L,
                        visibility = ContentVisibility.PRIVATE,
                        starterRecipe = starterRecipe,
                    ),
                ),
            ).toList().single(),
        ).plan

        val published = assertIs<MealPlansEvent.Internal.PlanSaved>(
            handler.handle(flowOf(MealPlansCommand.PublishPlan(created))).toList().single(),
        ).plan
        assertEquals(ContentVisibility.PUBLIC, published.visibility)

        val subscribed = assertIs<MealPlansEvent.Internal.ActionSucceeded>(
            handler.handle(flowOf(MealPlansCommand.SubscribeToPlan(UserId("student-1"), published.id))).toList().single(),
        )
        assertEquals(1, coachingRepo.grants.size)
        assertEquals("Подписка на меню оформлена", subscribed.message)

        val applied = assertIs<MealPlansEvent.Internal.ActionSucceeded>(
            handler.handle(flowOf(MealPlansCommand.ApplyPlanDay(published, 1))).toList().single(),
        )
        assertEquals("В дневник добавлено блюд: 1", applied.message)
        assertEquals(1, diaryRepo.addedRecords.size)
    }
}
