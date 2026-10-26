package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileUiStateMapperTest {

    private val mapper = ProfileUiStateMapper()
    private val testUserId = UserId("user-test-1")
    private val testProfile = Profile(
        id = ProfileId("prof-1"),
        userId = testUserId,
        heightCm = 180,
        weightKg = 75.0,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.LOSE_WEIGHT,
        nutritionGoal = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 150.0,
            targetFatsGrams = 60.0,
            targetCarbsGrams = 200.0,
            targetWeightKg = 70.0,
        ),
        weeklyBudget = 5000.0,
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `map Loading state returns UiState Loading`() {
        val state = ProfileState(content = ProfileState.Content.Loading)
        val uiState = mapper.map(state)
        assertIs<ProfileUiState.Content.Loading>(uiState.content)
    }

    @Test
    fun `map Error state returns UiState Error with same message`() {
        val state = ProfileState(content = ProfileState.Content.Error("Ошибка"))
        val uiState = mapper.map(state)
        assertIs<ProfileUiState.Content.Error>(uiState.content)
        assertEquals("Ошибка", (uiState.content as ProfileUiState.Content.Error).message)
    }

    @Test
    fun `map Success state formats all labels correctly`() {
        val state = ProfileState(content = ProfileState.Content.Success(testProfile))
        val uiState = mapper.map(state)

        assertIs<ProfileUiState.Content.Success>(uiState.content)
        val uiModel = (uiState.content as ProfileUiState.Content.Success).profile

        assertEquals("180 см", uiModel.heightLabel)
        assertEquals("75.0 кг", uiModel.weightLabel)
        assertEquals("Мужской", uiModel.genderLabel)
        assertEquals("Умеренная активность", uiModel.activityLabel)
        assertEquals("Снижение веса", uiModel.goalLabel)
        assertEquals("2000 ккал", uiModel.caloriesGoalLabel)
        assertEquals("150 г", uiModel.proteinsGoalLabel)
        assertEquals("60 г", uiModel.fatsGoalLabel)
        assertEquals("200 г", uiModel.carbsGoalLabel)
        assertEquals("70 кг", uiModel.weightGoalLabel)
        assertEquals("5000 ₽ / нед.", uiModel.weeklyBudgetLabel)
    }
}
