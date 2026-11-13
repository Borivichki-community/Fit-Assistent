package dev.andre.nutritionist.profile.persistence

import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import dev.andre.nutritionist.profile.persistence.entity.NutritionGoalEmbeddable
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import dev.andre.nutritionist.profile.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.util.UUID

class ProfilePersistenceTests : AbstractPostgresIntegrationTest() {
    @Autowired private lateinit var profileRepository: ProfileJpaRepository

    @BeforeEach
    fun setUp() {
        profileRepository.deleteAll()
    }

    @Test
    fun `liquibase migrations execute successfully and profile preserves all proto fields`() {
        val profileId = UUID.randomUUID()
        val profile =
            ProfileEntity(
                id = profileId,
                userId = "usr_kc_12345",
                heightCm = 180,
                weightKg = BigDecimal("75.50"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE_WEIGHT,
                nutritionGoal =
                    NutritionGoalEmbeddable(
                        targetCalories = 2200,
                        targetProteinsGrams = BigDecimal("150.00"),
                        targetFatsGrams = BigDecimal("70.00"),
                        targetCarbsGrams = BigDecimal("240.00"),
                        targetWeightKg = BigDecimal("70.00"),
                    ),
                weeklyBudget = BigDecimal("3500.00"),
            )
        val savedProfile = profileRepository.saveAndFlush(profile)
        assertEquals(profileId, savedProfile.id)
        assertEquals("usr_kc_12345", savedProfile.userId)
        assertEquals(2200, savedProfile.nutritionGoal.targetCalories)

        val fetchedProfile = profileRepository.findByUserId("usr_kc_12345")
        assertNotNull(fetchedProfile)
        assertEquals(GoalType.LOSE_WEIGHT, fetchedProfile?.goalType)
        assertEquals(BigDecimal("150.00"), fetchedProfile?.nutritionGoal?.targetProteinsGrams)
    }

    @Test
    fun `inserting profile with duplicate userId violates unique constraint`() {
        val profile1 =
            ProfileEntity(
                id = UUID.randomUUID(),
                userId = "duplicate_user",
                heightCm = 175,
                weightKg = BigDecimal("70.00"),
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = GoalType.MAINTAIN,
                nutritionGoal =
                    NutritionGoalEmbeddable(
                        targetCalories = 2000,
                        targetProteinsGrams = BigDecimal("120.00"),
                        targetFatsGrams = BigDecimal("60.00"),
                        targetCarbsGrams = BigDecimal("200.00"),
                        targetWeightKg = BigDecimal("68.00"),
                    ),
                weeklyBudget = BigDecimal("2500.00"),
            )
        profileRepository.saveAndFlush(profile1)

        val profile2 =
            ProfileEntity(
                id = UUID.randomUUID(),
                userId = "duplicate_user",
                heightCm = 180,
                weightKg = BigDecimal("72.00"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.GAIN_WEIGHT,
                nutritionGoal =
                    NutritionGoalEmbeddable(
                        targetCalories = 2500,
                        targetProteinsGrams = BigDecimal("160.00"),
                        targetFatsGrams = BigDecimal("70.00"),
                        targetCarbsGrams = BigDecimal("260.00"),
                        targetWeightKg = BigDecimal("75.00"),
                    ),
                weeklyBudget = BigDecimal("3000.00"),
            )
        assertThrows<DataIntegrityViolationException> {
            profileRepository.saveAndFlush(profile2)
        }
    }
}
