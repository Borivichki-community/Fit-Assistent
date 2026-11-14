package dev.andre.nutritionist.profile.mapper

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.domain.mapper.toCommand
import dev.andre.nutritionist.profile.domain.mapper.toDomain
import dev.andre.nutritionist.profile.domain.mapper.toEmbeddable
import dev.andre.nutritionist.profile.domain.mapper.toNewEntity
import dev.andre.nutritionist.profile.domain.mapper.toProto
import dev.andre.nutritionist.profile.domain.mapper.updateFrom
import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.persistence.entity.NutritionGoalEmbeddable
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ProfileDtoMapperTest {
    // ── Gender ────────────────────────────────────────────────────────────────

    @Test
    fun `gender proto maps to domain model correctly`() {
        assertEquals(Gender.MALE, GenderProto.GENDER_MALE.toDomain())
        assertEquals(Gender.FEMALE, GenderProto.GENDER_FEMALE.toDomain())
        assertEquals(Gender.UNKNOWN, GenderProto.GENDER_UNSPECIFIED.toDomain())
        assertEquals(Gender.UNKNOWN, GenderProto.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `gender domain maps to proto correctly`() {
        assertEquals(GenderProto.GENDER_MALE, Gender.MALE.toProto())
        assertEquals(GenderProto.GENDER_FEMALE, Gender.FEMALE.toProto())
        assertEquals(GenderProto.GENDER_UNSPECIFIED, Gender.UNKNOWN.toProto())
    }

    // ── ActivityLevel ─────────────────────────────────────────────────────────

    @Test
    fun `activity level proto maps to domain model correctly`() {
        assertEquals(ActivityLevel.SEDENTARY, ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY.toDomain())
        assertEquals(ActivityLevel.LIGHT, ActivityLevelProto.ACTIVITY_LEVEL_LIGHT.toDomain())
        assertEquals(ActivityLevel.MODERATE, ActivityLevelProto.ACTIVITY_LEVEL_MODERATE.toDomain())
        assertEquals(ActivityLevel.VERY_ACTIVE, ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE.toDomain())
        assertEquals(ActivityLevel.UNKNOWN, ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED.toDomain())
        assertEquals(ActivityLevel.UNKNOWN, ActivityLevelProto.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `activity level domain maps to proto correctly`() {
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY, ActivityLevel.SEDENTARY.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_LIGHT, ActivityLevel.LIGHT.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE, ActivityLevel.MODERATE.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE, ActivityLevel.VERY_ACTIVE.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED, ActivityLevel.UNKNOWN.toProto())
    }

    // ── GoalType ──────────────────────────────────────────────────────────────

    @Test
    fun `goal type proto maps to domain model correctly`() {
        assertEquals(GoalType.LOSE_WEIGHT, GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT.toDomain())
        assertEquals(GoalType.MAINTAIN, GoalTypeProto.GOAL_TYPE_MAINTAIN.toDomain())
        assertEquals(GoalType.GAIN_WEIGHT, GoalTypeProto.GOAL_TYPE_GAIN_WEIGHT.toDomain())
        assertEquals(GoalType.UNKNOWN, GoalTypeProto.GOAL_TYPE_UNSPECIFIED.toDomain())
        assertEquals(GoalType.UNKNOWN, GoalTypeProto.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `goal type domain maps to proto correctly`() {
        assertEquals(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT, GoalType.LOSE_WEIGHT.toProto())
        assertEquals(GoalTypeProto.GOAL_TYPE_MAINTAIN, GoalType.MAINTAIN.toProto())
        assertEquals(GoalTypeProto.GOAL_TYPE_GAIN_WEIGHT, GoalType.GAIN_WEIGHT.toProto())
        assertEquals(GoalTypeProto.GOAL_TYPE_UNSPECIFIED, GoalType.UNKNOWN.toProto())
    }

    // ── ProfileEntity -> ProfileProto ─────────────────────────────────────────

    @Test
    fun `profile entity maps to profile proto with all fields and timestamp`() {
        val id = UUID.randomUUID()
        val now = Instant.parse("2026-11-12T12:00:00Z")
        val entity =
            ProfileEntity(
                id = id,
                userId = "usr_mapper_test",
                heightCm = 185,
                weightKg = BigDecimal("84.50"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.LOSE_WEIGHT,
                nutritionGoal =
                    NutritionGoalEmbeddable(
                        targetCalories = 2400,
                        targetProteinsGrams = BigDecimal("165.00"),
                        targetFatsGrams = BigDecimal("70.00"),
                        targetCarbsGrams = BigDecimal("270.00"),
                        targetWeightKg = BigDecimal("78.00"),
                    ),
                weeklyBudget = BigDecimal("4200.00"),
                createdAt = now,
                updatedAt = now,
            )

        val proto = entity.toProto()
        assertEquals(id.toString(), proto.id)
        assertEquals("usr_mapper_test", proto.userId)
        assertEquals(185, proto.heightCm)
        assertEquals(84.50, proto.weightKg)
        assertEquals(GenderProto.GENDER_MALE, proto.gender)
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE, proto.activityLevel)
        assertEquals(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT, proto.goalType)
        assertEquals(4200.0, proto.weeklyBudget)
        assertEquals(2400, proto.nutritionGoal.targetCalories)
        assertEquals(now.epochSecond, proto.updatedAt.seconds)
    }

    // ── Domain NutritionGoal <-> Proto & Embeddable ──────────────────────────

    @Test
    fun `domain nutrition goal maps to proto and back correctly`() {
        val domain =
            NutritionGoal(
                targetCalories = 2400,
                targetProteinsGrams = BigDecimal("160.00"),
                targetFatsGrams = BigDecimal("70.00"),
                targetCarbsGrams = BigDecimal("280.00"),
                targetWeightKg = BigDecimal("75.00"),
            )

        val proto = domain.toProto()
        assertEquals(2400, proto.targetCalories)
        assertEquals(160.0, proto.targetProteinsGrams)
        assertEquals(70.0, proto.targetFatsGrams)
        assertEquals(280.0, proto.targetCarbsGrams)
        assertEquals(75.0, proto.targetWeightKg)

        val mappedBack = proto.toDomain()
        assertEquals(domain.targetCalories, mappedBack.targetCalories)
        assertEquals(0, domain.targetProteinsGrams.compareTo(mappedBack.targetProteinsGrams))
        assertEquals(0, domain.targetFatsGrams.compareTo(mappedBack.targetFatsGrams))
        assertEquals(0, domain.targetCarbsGrams.compareTo(mappedBack.targetCarbsGrams))
        assertEquals(0, domain.targetWeightKg.compareTo(mappedBack.targetWeightKg))
    }

    @Test
    fun `domain nutrition goal maps to embeddable and back correctly`() {
        val domain =
            NutritionGoal(
                targetCalories = 2100,
                targetProteinsGrams = BigDecimal("145.00"),
                targetFatsGrams = BigDecimal("65.00"),
                targetCarbsGrams = BigDecimal("230.00"),
                targetWeightKg = BigDecimal("72.00"),
            )

        val embeddable = domain.toEmbeddable()
        assertEquals(2100, embeddable.targetCalories)
        assertEquals(domain.targetProteinsGrams, embeddable.targetProteinsGrams)
        assertEquals(domain.targetFatsGrams, embeddable.targetFatsGrams)
        assertEquals(domain.targetCarbsGrams, embeddable.targetCarbsGrams)
        assertEquals(domain.targetWeightKg, embeddable.targetWeightKg)

        val mappedBack = embeddable.toDomain()
        assertEquals(domain, mappedBack)
    }

    // ── UpdateProfileRequest.toCommand ───────────────────────────────────────

    @Test
    fun `toCommand maps UpdateProfileRequest with explicit goal to UpsertProfileCommand`() {
        val request =
            UpdateProfileRequest
                .newBuilder()
                .setUserId("user_cmd_1")
                .setHeightCm(180)
                .setWeightKg(80.0)
                .setGender(GenderProto.GENDER_MALE)
                .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY)
                .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                .setWeeklyBudget(5000.0)
                .setNutritionGoal(
                    NutritionGoalProto
                        .newBuilder()
                        .setTargetCalories(2500)
                        .setTargetProteinsGrams(150.0)
                        .setTargetFatsGrams(70.0)
                        .setTargetCarbsGrams(300.0)
                        .setTargetWeightKg(80.0)
                        .build(),
                ).build()

        val command = request.toCommand()
        assertEquals(UserId("user_cmd_1"), command.userId)
        assertEquals(180, command.heightCm)
        assertEquals(0, BigDecimal("80.00").compareTo(command.weightKg))
        assertEquals(Gender.MALE, command.gender)
        assertEquals(ActivityLevel.SEDENTARY, command.activityLevel)
        assertEquals(GoalType.MAINTAIN, command.goalType)
        assertEquals(0, BigDecimal("5000.00").compareTo(command.weeklyBudget))
        assertNotNull(command.nutritionGoal)
        assertEquals(2500, command.nutritionGoal?.targetCalories)
    }

    @Test
    fun `toCommand maps UpdateProfileRequest without goal to UpsertProfileCommand with null goal`() {
        val request =
            UpdateProfileRequest
                .newBuilder()
                .setUserId("user_cmd_2")
                .setHeightCm(175)
                .setWeightKg(70.0)
                .setGender(GenderProto.GENDER_FEMALE)
                .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                .setGoalType(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT)
                .setWeeklyBudget(3500.0)
                .build()

        val command = request.toCommand()
        assertEquals(UserId("user_cmd_2"), command.userId)
        assertNull(command.nutritionGoal)
    }

    // ── UpsertProfileCommand.toNewEntity & ProfileEntity.updateFrom ──────────

    @Test
    fun `toNewEntity creates ProfileEntity from command and finalGoal with UUIDv7`() {
        val command =
            UpsertProfileCommand(
                userId = UserId("user_new_entity_test"),
                heightCm = 178,
                weightKg = BigDecimal("76.00"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.MAINTAIN,
                nutritionGoal = null,
                weeklyBudget = BigDecimal("4000.00"),
            )
        val finalGoal =
            NutritionGoal(
                targetCalories = 2300,
                targetProteinsGrams = BigDecimal("150.00"),
                targetFatsGrams = BigDecimal("70.00"),
                targetCarbsGrams = BigDecimal("260.00"),
                targetWeightKg = BigDecimal("76.00"),
            )

        val entity = command.toNewEntity(finalGoal)

        assertNotNull(entity.id)
        assertEquals(7, entity.id.version())
        assertEquals("user_new_entity_test", entity.userId)
        assertEquals(178, entity.heightCm)
        assertEquals(BigDecimal("76.00"), entity.weightKg)
        assertEquals(Gender.MALE, entity.gender)
        assertEquals(ActivityLevel.MODERATE, entity.activityLevel)
        assertEquals(GoalType.MAINTAIN, entity.goalType)
        assertEquals(2300, entity.nutritionGoal.targetCalories)
        assertEquals(BigDecimal("4000.00"), entity.weeklyBudget)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    @Test
    fun `updateFrom updates existing ProfileEntity in-place preserving id and createdAt`() {
        val id = UUID.randomUUID()
        val originalCreated = Instant.parse("2026-11-01T10:00:00Z")
        val existing =
            ProfileEntity(
                id = id,
                userId = "user_update_test",
                heightCm = 170,
                weightKg = BigDecimal("70.00"),
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = GoalType.LOSE_WEIGHT,
                nutritionGoal = NutritionGoalEmbeddable(targetCalories = 1900),
                weeklyBudget = BigDecimal("3000.00"),
                createdAt = originalCreated,
                updatedAt = originalCreated,
            )

        val command =
            UpsertProfileCommand(
                userId = UserId("user_update_test"),
                heightCm = 172,
                weightKg = BigDecimal("68.50"),
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.MODERATE,
                goalType = GoalType.MAINTAIN,
                nutritionGoal = null,
                weeklyBudget = BigDecimal("3500.00"),
            )
        val finalGoal =
            NutritionGoal(
                targetCalories = 2100,
                targetProteinsGrams = BigDecimal("130.00"),
                targetFatsGrams = BigDecimal("60.00"),
                targetCarbsGrams = BigDecimal("240.00"),
                targetWeightKg = BigDecimal("68.00"),
            )

        val updated = existing.updateFrom(command, finalGoal)

        assertEquals(id, updated.id)
        assertEquals(originalCreated, updated.createdAt)
        assertEquals(172, updated.heightCm)
        assertEquals(BigDecimal("68.50"), updated.weightKg)
        assertEquals(ActivityLevel.MODERATE, updated.activityLevel)
        assertEquals(GoalType.MAINTAIN, updated.goalType)
        assertEquals(2100, updated.nutritionGoal.targetCalories)
        assertEquals(BigDecimal("3500.00"), updated.weeklyBudget)
        assertTrue(updated.updatedAt.isAfter(originalCreated))
    }
}
