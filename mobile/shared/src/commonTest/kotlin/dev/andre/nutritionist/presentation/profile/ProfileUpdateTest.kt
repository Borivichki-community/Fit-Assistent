package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileUpdateTest {

    private val update = ProfileUpdate()
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
        updatedAt = kotlin.time.Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `LoadProfile event sets Loading state and produces LoadProfile command`() {
        val initialState = ProfileState()

        val next = update.update(initialState, ProfileEvent.Ui.LoadProfile(testUserId))

        assertIs<ProfileState.Content.Loading>(next.state?.content)
        assertEquals(testUserId, next.state?.currentUserId)
        assertEquals(listOf(ProfileCommand.LoadProfile(testUserId)), next.commands)
    }

    @Test
    fun `Retry event reloads profile when currentUserId is present`() {
        val errorState = ProfileState(
            content = ProfileState.Content.Error("Ошибка"),
            currentUserId = testUserId,
        )

        val next = update.update(errorState, ProfileEvent.Ui.Retry)

        assertIs<ProfileState.Content.Loading>(next.state?.content)
        assertEquals(listOf(ProfileCommand.LoadProfile(testUserId)), next.commands)
    }

    @Test
    fun `ProfileLoaded internal event transitions state to Success`() {
        val loadingState = ProfileState(content = ProfileState.Content.Loading, currentUserId = testUserId)

        val next = update.update(loadingState, ProfileEvent.Internal.ProfileLoaded(testProfile))

        assertIs<ProfileState.Content.Success>(next.state?.content)
        assertEquals(testProfile, (next.state?.content as ProfileState.Content.Success).profile)
    }

    @Test
    fun `ProfileLoadFailed internal event transitions state to Error and emits News`() {
        val loadingState = ProfileState(content = ProfileState.Content.Loading, currentUserId = testUserId)
        val error = RuntimeException("Сетевая ошибка")

        val next = update.update(loadingState, ProfileEvent.Internal.ProfileLoadFailed(error))

        assertIs<ProfileState.Content.Error>(next.state?.content)
        assertEquals("Сетевая ошибка", (next.state?.content as ProfileState.Content.Error).message)
        assertEquals(listOf(ProfileNews.ShowSnackbar("Сетевая ошибка")), next.news)
    }

    @Test
    fun `ProfileLoadFailed internal event with null message transitions state to Error with default message`() {
        val loadingState = ProfileState(content = ProfileState.Content.Loading, currentUserId = testUserId)
        val error = RuntimeException()

        val next = update.update(loadingState, ProfileEvent.Internal.ProfileLoadFailed(error))

        assertIs<ProfileState.Content.Error>(next.state?.content)
        assertEquals("Не удалось загрузить профиль", (next.state?.content as ProfileState.Content.Error).message)
        assertEquals(listOf(ProfileNews.ShowSnackbar("Не удалось загрузить профиль")), next.news)
    }

    @Test
    fun `ProfileLoadFailed internal event with blank message transitions state to Error with default message`() {
        val loadingState = ProfileState(content = ProfileState.Content.Loading, currentUserId = testUserId)
        val error = RuntimeException("   ")

        val next = update.update(loadingState, ProfileEvent.Internal.ProfileLoadFailed(error))

        assertIs<ProfileState.Content.Error>(next.state?.content)
        assertEquals("Не удалось загрузить профиль", (next.state?.content as ProfileState.Content.Error).message)
        assertEquals(listOf(ProfileNews.ShowSnackbar("Не удалось загрузить профиль")), next.news)
    }

    @Test
    fun `Retry event when currentUserId is null does nothing`() {
        val state = ProfileState(
            content = ProfileState.Content.Error("Ошибка"),
            currentUserId = null,
        )

        val next = update.update(state, ProfileEvent.Ui.Retry)

        assertEquals(null, next.state)
        assertEquals(emptyList(), next.commands)
        assertEquals(emptyList(), next.news)
    }

    @Test
    fun `EditClicked event produces NavigateToEdit news`() {
        val successState = ProfileState(
            content = ProfileState.Content.Success(testProfile),
            currentUserId = testUserId,
        )

        val next = update.update(successState, ProfileEvent.Ui.EditClicked)

        assertEquals(listOf(ProfileNews.NavigateToEdit(testUserId)), next.news)
    }

    @Test
    fun `EditClicked event when currentUserId is null does nothing`() {
        val state = ProfileState(
            content = ProfileState.Content.Success(testProfile),
            currentUserId = null,
        )

        val next = update.update(state, ProfileEvent.Ui.EditClicked)

        assertEquals(null, next.state)
        assertEquals(emptyList(), next.commands)
        assertEquals(emptyList(), next.news)
    }

    @Test
    fun `SaveOnboardingProfile event produces UpdateProfile command and sets Loading state with isSaving true`() {
        val initialState = ProfileState()
        val onboardingData = dev.andre.nutritionist.domain.model.OnboardingData(
            firstName = "Aleksey",
            lastName = "Codintsev",
            heightCm = "180",
            weightKg = "80.0",
            age = "25",
        )

        val next = update.update(initialState, ProfileEvent.Ui.SaveOnboardingProfile(onboardingData, testUserId))

        assertIs<ProfileState.Content.Loading>(next.state?.content)
        assertEquals(testUserId, next.state?.currentUserId)
        assertEquals(true, next.state?.isSaving)
        assertEquals("Aleksey Codintsev", next.state?.displayName)
        assertEquals("25", next.state?.age)
        assertEquals(1, next.commands.size)
        assertIs<ProfileCommand.UpdateProfile>(next.commands.first())
    }

    @Test
    fun `ProfileLoaded event when isSaving true produces ProfileSaved news`() {
        val savingState = ProfileState(
            content = ProfileState.Content.Loading,
            currentUserId = testUserId,
            isSaving = true,
            displayName = "Aleksey",
        )

        val next = update.update(savingState, ProfileEvent.Internal.ProfileLoaded(testProfile))

        assertIs<ProfileState.Content.Success>(next.state?.content)
        assertEquals(false, next.state?.isSaving)
        assertEquals("Aleksey", (next.state?.content as ProfileState.Content.Success).displayName)
        assertEquals(listOf(ProfileNews.ProfileSaved), next.news)
    }

    @Test
    fun `UpdateProfile event produces UpdateProfile command and sets Loading state`() {
        val state = ProfileState(content = ProfileState.Content.Success(testProfile))

        val next = update.update(state, ProfileEvent.Ui.UpdateProfile(testProfile))

        assertIs<ProfileState.Content.Loading>(next.state?.content)
        assertEquals(true, next.state?.isSaving)
        assertEquals(listOf(ProfileCommand.UpdateProfile(testProfile)), next.commands)
    }

    @Test
    fun `ChangeWeight event produces UpdateProfile command with mutated weightKg, empty nutritionGoal and sets isSaving true`() {
        val state = ProfileState(content = ProfileState.Content.Success(testProfile))

        val next = update.update(state, ProfileEvent.Ui.ChangeWeight(80.5))

        assertEquals(true, next.state?.isSaving)
        assertEquals(1, next.commands.size)
        val command = next.commands.first() as ProfileCommand.UpdateProfile
        assertEquals(80.5, command.profile.weightKg)
        assertEquals(testProfile.activityLevel, command.profile.activityLevel)
        assertEquals(NutritionGoal.EMPTY, command.profile.nutritionGoal)
    }

    @Test
    fun `ChangeActivity event produces UpdateProfile command with mutated activityLevel, empty nutritionGoal and sets isSaving true`() {
        val state = ProfileState(content = ProfileState.Content.Success(testProfile))

        val next = update.update(state, ProfileEvent.Ui.ChangeActivity(ActivityLevel.VERY_ACTIVE))

        assertEquals(true, next.state?.isSaving)
        assertEquals(1, next.commands.size)
        val command = next.commands.first() as ProfileCommand.UpdateProfile
        assertEquals(ActivityLevel.VERY_ACTIVE, command.profile.activityLevel)
        assertEquals(testProfile.weightKg, command.profile.weightKg)
        assertEquals(NutritionGoal.EMPTY, command.profile.nutritionGoal)
    }

    @Test
    fun `ChangeGoal event produces UpdateProfile command with mutated goalType, empty nutritionGoal and sets isSaving true`() {
        val state = ProfileState(content = ProfileState.Content.Success(testProfile))

        val next = update.update(state, ProfileEvent.Ui.ChangeGoal(GoalType.GAIN_WEIGHT))

        assertEquals(true, next.state?.isSaving)
        assertEquals(1, next.commands.size)
        val command = next.commands.first() as ProfileCommand.UpdateProfile
        assertEquals(GoalType.GAIN_WEIGHT, command.profile.goalType)
        assertEquals(NutritionGoal.EMPTY, command.profile.nutritionGoal)
    }

    @Test
    fun `ChangeBudget event produces UpdateProfile command with mutated weeklyBudget and preserves nutritionGoal`() {
        val state = ProfileState(content = ProfileState.Content.Success(testProfile))

        val next = update.update(state, ProfileEvent.Ui.ChangeBudget(7500.0))

        assertEquals(true, next.state?.isSaving)
        assertEquals(1, next.commands.size)
        val command = next.commands.first() as ProfileCommand.UpdateProfile
        assertEquals(7500.0, command.profile.weeklyBudget)
        assertEquals(testProfile.nutritionGoal, command.profile.nutritionGoal)
    }
}

