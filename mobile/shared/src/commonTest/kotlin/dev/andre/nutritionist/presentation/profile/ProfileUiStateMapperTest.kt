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

    @Test
    fun `map Success state formats labels correctly for UNKNOWN enum values`() {
        val unknownProfile = testProfile.copy(
            gender = Gender.UNKNOWN,
            activityLevel = ActivityLevel.UNKNOWN,
            goalType = GoalType.UNKNOWN,
        )
        val state = ProfileState(content = ProfileState.Content.Success(unknownProfile))
        val uiState = mapper.map(state)

        assertIs<ProfileUiState.Content.Success>(uiState.content)
        val uiModel = (uiState.content as ProfileUiState.Content.Success).profile

        assertEquals("Не указан", uiModel.genderLabel)
        assertEquals("Не указано", uiModel.activityLabel)
        assertEquals("Не указано", uiModel.goalLabel)
    }

    @Test
    fun `map Success state formats labels correctly for fractional weights and zero values`() {
        val zeroProfile = testProfile.copy(
            weightKg = 75.5,
            weeklyBudget = 0.0,
            nutritionGoal = testProfile.nutritionGoal.copy(
                targetCalories = 0,
                targetProteinsGrams = 0.0,
                targetFatsGrams = 0.0,
                targetCarbsGrams = 0.0,
                targetWeightKg = 0.0,
            )
        )
        val state = ProfileState(content = ProfileState.Content.Success(zeroProfile))
        val uiState = mapper.map(state)

        assertIs<ProfileUiState.Content.Success>(uiState.content)
        val uiModel = (uiState.content as ProfileUiState.Content.Success).profile

        assertEquals("75.5 кг", uiModel.weightLabel)
        assertEquals("0 ккал", uiModel.caloriesGoalLabel)
        assertEquals("0 г", uiModel.proteinsGoalLabel)
        assertEquals("0 г", uiModel.fatsGoalLabel)
        assertEquals("0 г", uiModel.carbsGoalLabel)
        assertEquals("0 кг", uiModel.weightGoalLabel)
        assertEquals("0 ₽ / нед.", uiModel.weeklyBudgetLabel)
    }
}
