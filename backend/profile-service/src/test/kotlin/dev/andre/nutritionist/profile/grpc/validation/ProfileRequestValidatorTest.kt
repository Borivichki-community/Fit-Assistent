package dev.andre.nutritionist.profile.grpc.validation

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import com.google.rpc.BadRequest
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ProfileRequestValidatorTest {
    private val validator = ProfileRequestValidator()

    data class InvalidCase(
        val name: String,
        val request: UpdateProfileRequest,
        val field: String,
        val code: ViolationCode,
    ) {
        override fun toString() = name
    }

    companion object {
        private fun validUpdate() =
            UpdateProfileRequest
                .newBuilder()
                .setUserId("user-1")
                .setHeightCm(180)
                .setWeightKg(75.0)
                .setWeeklyBudget(100.0)

        @JvmStatic
        fun invalidUpdates() =
            listOf(
                InvalidCase(
                    "blank user_id",
                    validUpdate().setUserId(" ").build(),
                    "user_id",
                    ViolationCode.REQUIRED,
                ),
                InvalidCase(
                    "height is zero",
                    validUpdate().setHeightCm(0).build(),
                    "height_cm",
                    ViolationCode.OUT_OF_RANGE,
                ),
                InvalidCase(
                    "height is excessively large",
                    validUpdate().setHeightCm(350).build(),
                    "height_cm",
                    ViolationCode.OUT_OF_RANGE,
                ),
                InvalidCase(
                    "weight is NaN",
                    validUpdate().setWeightKg(Double.NaN).build(),
                    "weight_kg",
                    ViolationCode.NOT_FINITE,
                ),
                InvalidCase(
                    "weight is infinite",
                    validUpdate().setWeightKg(Double.POSITIVE_INFINITY).build(),
                    "weight_kg",
                    ViolationCode.NOT_FINITE,
                ),
                InvalidCase(
                    "weight is below minimum",
                    validUpdate().setWeightKg(15.0).build(),
                    "weight_kg",
                    ViolationCode.OUT_OF_RANGE,
                ),
                InvalidCase(
                    "budget is negative",
                    validUpdate().setWeeklyBudget(-1.0).build(),
                    "weekly_budget",
                    ViolationCode.OUT_OF_RANGE,
                ),
                InvalidCase(
                    "budget is infinite",
                    validUpdate().setWeeklyBudget(Double.POSITIVE_INFINITY).build(),
                    "weekly_budget",
                    ViolationCode.NOT_FINITE,
                ),
                InvalidCase(
                    "budget is excessively large",
                    validUpdate().setWeeklyBudget(2_000_000.0).build(),
                    "weekly_budget",
                    ViolationCode.OUT_OF_RANGE,
                ),
                InvalidCase(
                    "negative proteins in nutrition goal",
                    validUpdate()
                        .setNutritionGoal(NutritionGoalProto.newBuilder().setTargetProteinsGrams(-1.0))
                        .build(),
                    "nutrition_goal.target_proteins_grams",
                    ViolationCode.OUT_OF_RANGE,
                ),
                InvalidCase(
                    "NaN carbs in nutrition goal",
                    validUpdate()
                        .setNutritionGoal(NutritionGoalProto.newBuilder().setTargetCarbsGrams(Double.NaN))
                        .build(),
                    "nutrition_goal.target_carbs_grams",
                    ViolationCode.NOT_FINITE,
                ),
            )
    }

    private fun StatusException.violations(): List<Pair<String, String>> {
        val status =
            requireNotNull(StatusProto.fromThrowable(this)) {
                "StatusProto must not be null"
            }
        val badRequest = status.detailsList.firstOrNull()?.unpack(BadRequest::class.java)
        return badRequest?.fieldViolationsList?.map { it.field to it.reason } ?: emptyList()
    }

    @Test
    fun `valid update request passes validation`() {
        validator.validate(validUpdate().build())
    }

    @Test
    fun `valid get profile request passes validation`() {
        validator.validate(GetProfileRequest.newBuilder().setUserId("user_valid_123").build())
    }

    @Test
    fun `get profile request with blank user_id throws REQUIRED violation`() {
        val ex =
            assertThrows<StatusException> {
                validator.validate(GetProfileRequest.newBuilder().setUserId("   ").build())
            }

        assertEquals(Status.Code.INVALID_ARGUMENT, ex.status.code)
        assertTrue(("user_id" to ViolationCode.REQUIRED.name) in ex.violations())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUpdates")
    fun `invalid update request is rejected with field violation`(case: InvalidCase) {
        val ex = assertThrows<StatusException> { validator.validate(case.request) }

        assertEquals(Status.Code.INVALID_ARGUMENT, ex.status.code)
        assertTrue(
            (case.field to case.code.name) in ex.violations(),
            "Expected violation (${case.field}, ${case.code.name}), actual: ${ex.violations()}",
        )
    }

    @Test
    fun `all violations are reported at once`() {
        val request =
            validUpdate()
                .setHeightCm(0)
                .setWeightKg(Double.NaN)
                .setWeeklyBudget(-50.0)
                .build()

        val ex = assertThrows<StatusException> { validator.validate(request) }

        val violations = ex.violations()
        assertTrue(("height_cm" to ViolationCode.OUT_OF_RANGE.name) in violations)
        assertTrue(("weight_kg" to ViolationCode.NOT_FINITE.name) in violations)
        assertTrue(("weekly_budget" to ViolationCode.OUT_OF_RANGE.name) in violations)
    }
}
