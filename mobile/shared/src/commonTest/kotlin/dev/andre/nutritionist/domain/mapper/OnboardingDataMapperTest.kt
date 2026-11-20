package dev.andre.nutritionist.domain.mapper

import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.OnboardingData
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingDataMapperTest {

    private val testUserId = UserId("user-test-42")
    private val testTimestamp = kotlin.time.Instant.fromEpochMilliseconds(1_700_000_000_000L)

    @Test
    fun `toProfile maps onboarding data correctly with empty nutrition goals for backend calculation`() {
        val onboardingData = OnboardingData(
            firstName = "Aleksey",
            lastName = "Codintsev",
            heightCm = "180",
            weightKg = "80.0",
            age = "25",
            gender = Gender.MALE,
            activityLevel = ActivityLevel.SEDENTARY,
            weeklyBudget = "6500",
            goalType = GoalType.MAINTAIN,
        )

        val profile = onboardingData.toProfile(
            userId = testUserId,
            profileId = ProfileId("p-custom-1"),
            updatedAt = testTimestamp,
        )

        assertEquals(ProfileId("p-custom-1"), profile.id)
        assertEquals(testUserId, profile.userId)
        assertEquals(180, profile.heightCm)
        assertEquals(80.0, profile.weightKg)
        assertEquals(Gender.MALE, profile.gender)
        assertEquals(ActivityLevel.SEDENTARY, profile.activityLevel)
        assertEquals(GoalType.MAINTAIN, profile.goalType)
        assertEquals(6500.0, profile.weeklyBudget)
        assertEquals(testTimestamp, profile.updatedAt)

        // Nutrition goals should be empty/zero - profile-service is SSOT and calculates them
        assertEquals(NutritionGoal.EMPTY, profile.nutritionGoal)
        assertEquals(0, profile.nutritionGoal.targetCalories)
        assertEquals(0.0, profile.nutritionGoal.targetProteinsGrams)
        assertEquals(0.0, profile.nutritionGoal.targetFatsGrams)
        assertEquals(0.0, profile.nutritionGoal.targetCarbsGrams)
    }

    @Test
    fun `toProfile handles empty fields with safe defaults`() {
        val emptyData = OnboardingData(
            firstName = "",
            lastName = "",
            heightCm = "",
            weightKg = "",
            age = "",
            weeklyBudget = "",
        )

        val profile = emptyData.toProfile(userId = testUserId)

        assertEquals(testUserId, profile.userId)
        assertEquals(170, profile.heightCm)
        assertEquals(70.0, profile.weightKg)
        assertEquals(5000.0, profile.weeklyBudget)
        assertEquals(ProfileId("p-user-test-42"), profile.id)
        assertEquals(NutritionGoal.EMPTY, profile.nutritionGoal)
    }
}
