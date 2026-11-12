package dev.andre.nutritionist.profile.mapper

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.domain.mapper.applyUpdates
import dev.andre.nutritionist.profile.domain.mapper.toDomain
import dev.andre.nutritionist.profile.domain.mapper.toEmbeddable
import dev.andre.nutritionist.profile.domain.mapper.toNewEntity
import dev.andre.nutritionist.profile.domain.mapper.toProto
import dev.andre.nutritionist.profile.domain.model.ActivityLevel
import dev.andre.nutritionist.profile.domain.model.Gender
import dev.andre.nutritionist.profile.domain.model.GoalType
import dev.andre.nutritionist.profile.persistence.entity.NutritionGoalEmbeddable
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

    // ── NutritionGoalEmbeddable <-> NutritionGoalProto ────────────────────────

    @Test
    fun `nutrition goal embeddable maps to proto correctly`() {
        val embeddable =
            NutritionGoalEmbeddable(
                targetCalories = 2300,
                targetProteinsGrams = BigDecimal("155.50"),
                targetFatsGrams = BigDecimal("72.25"),
                targetCarbsGrams = BigDecimal("260.00"),
                targetWeightKg = BigDecimal("73.50"),
            )
        val proto = embeddable.toProto()

        assertEquals(2300, proto.targetCalories)
        assertEquals(155.50, proto.targetProteinsGrams)
        assertEquals(72.25, proto.targetFatsGrams)
        assertEquals(260.00, proto.targetCarbsGrams)
        assertEquals(73.50, proto.targetWeightKg)
    }

    @Test
    fun `nutrition goal proto maps to embeddable correctly`() {
        val proto =
            NutritionGoalProto
                .newBuilder()
                .setTargetCalories(2500)
                .setTargetProteinsGrams(160.0)
                .setTargetFatsGrams(75.5)
                .setTargetCarbsGrams(280.25)
                .setTargetWeightKg(78.0)
                .build()
        val embeddable = proto.toEmbeddable()

        assertEquals(2500, embeddable.targetCalories)
        assertEquals(0, BigDecimal("160.00").compareTo(embeddable.targetProteinsGrams))
        assertEquals(0, BigDecimal("75.50").compareTo(embeddable.targetFatsGrams))
        assertEquals(0, BigDecimal("280.25").compareTo(embeddable.targetCarbsGrams))
        assertEquals(0, BigDecimal("78.00").compareTo(embeddable.targetWeightKg))
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

    // ── UpdateProfileRequest -> toNewEntity & applyUpdates ───────────────────

    @Test
    fun `update request converts to new entity with fresh UUID and timestamps`() {
        val request =
            UpdateProfileRequest
                .newBuilder()
                .setUserId("user_new_entity")
                .setHeightCm(170)
                .setWeightKg(68.0)
                .setGender(GenderProto.GENDER_FEMALE)
                .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY)
                .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                .setWeeklyBudget(3500.0)
                .setNutritionGoal(
                    NutritionGoalProto
                        .newBuilder()
                        .setTargetCalories(1800)
                        .setTargetProteinsGrams(110.0)
                        .setTargetFatsGrams(55.0)
                        .setTargetCarbsGrams(200.0)
                        .setTargetWeightKg(68.0)
                        .build(),
                ).build()

        val entity = request.toNewEntity()
        assertNotNull(entity.id)
        assertEquals(7, entity.id.version())
        assertEquals("user_new_entity", entity.userId)
        assertEquals(170, entity.heightCm)
        assertEquals(0, BigDecimal("68.00").compareTo(entity.weightKg))
        assertEquals(Gender.FEMALE, entity.gender)
        assertEquals(ActivityLevel.SEDENTARY, entity.activityLevel)
        assertEquals(GoalType.MAINTAIN, entity.goalType)
        assertEquals(0, BigDecimal("3500.00").compareTo(entity.weeklyBudget))
        assertEquals(1800, entity.nutritionGoal.targetCalories)
    }

    @Test
    fun `existing profile entity updates in-place via applyUpdates`() {
        val originalId = UUID.randomUUID()
        val originalCreated = Instant.parse("2026-11-01T00:00:00Z")
        val entity =
            ProfileEntity(
                id = originalId,
                userId = "user_in_place",
                heightCm = 175,
                weightKg = BigDecimal("75.00"),
                gender = Gender.MALE,
                activityLevel = ActivityLevel.LIGHT,
                goalType = GoalType.LOSE_WEIGHT,
                nutritionGoal = NutritionGoalEmbeddable(targetCalories = 2000),
                weeklyBudget = BigDecimal("3000.00"),
                createdAt = originalCreated,
                updatedAt = originalCreated,
            )

        val request =
            UpdateProfileRequest
                .newBuilder()
                .setUserId("user_in_place")
                .setHeightCm(176)
                .setWeightKg(73.5)
                .setGender(GenderProto.GENDER_MALE)
                .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                .setWeeklyBudget(4000.0)
                .setNutritionGoal(
                    NutritionGoalProto
                        .newBuilder()
                        .setTargetCalories(2200)
                        .setTargetProteinsGrams(140.0)
                        .setTargetFatsGrams(65.0)
                        .setTargetCarbsGrams(240.0)
                        .setTargetWeightKg(73.0)
                        .build(),
                ).build()

        val updated = entity.applyUpdates(request)
        assertEquals(originalId, updated.id)
        assertEquals(originalCreated, updated.createdAt)
        assertEquals(176, updated.heightCm)
        assertEquals(0, BigDecimal("73.50").compareTo(updated.weightKg))
        assertEquals(ActivityLevel.MODERATE, updated.activityLevel)
        assertEquals(GoalType.MAINTAIN, updated.goalType)
        assertEquals(0, BigDecimal("4000.00").compareTo(updated.weeklyBudget))
        assertEquals(2200, updated.nutritionGoal.targetCalories)
    }
}
