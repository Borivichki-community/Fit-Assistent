package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeCoachingRepository
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.FakeMealPlanRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class CoachStudentMenuFlowTest {

    @Test
    fun `handler loads student diary via getRange and grants then revokes plan access`() = runTest {
        val studentId = UserId("student-42")
        val recordTime = Instant.parse("2026-11-26T10:00:00Z")
        val coachingRepo = FakeCoachingRepository()
        val planRepo = FakeMealPlanRepository().apply {
            plans.add(
                MealPlanTemplate(
                    id = MealPlanTemplateId("plan-week-1"),
                    authorId = UserId("coach-1"),
                    title = "План на неделю",
                    durationDays = 7,
                ),
            )
        }
        val diaryRepo = FakeDiaryRepository().apply {
            recordsByUser[studentId.raw] = listOf(
                MealRecord(
                    id = MealRecordId("rec-1"),
                    mealType = MealType.BREAKFAST,
                    recordedAt = recordTime,
                    sourceType = EntrySourceType.CUSTOM,
                    sourceId = null,
                    nameSnapshot = "Овсянка",
                    weightGrams = 250.0,
                    nutrition = NutritionFacts(320.0, 12.0, 6.0, 52.0),
                    idempotencyKey = IdempotencyKey("k1"),
                ),
            )
        }
        val handler = CoachStudentMenuCommandsFlowHandler(
            coachingRepository = coachingRepo,
            mealPlanRepository = planRepo,
            diaryRepository = diaryRepo,
            dispatchers = TestCoroutineDispatchers(),
            nowProvider = { recordTime },
        )

        val loaded = assertIs<CoachStudentMenuEvent.Internal.Loaded>(
            handler.handle(
                flowOf(
                    CoachStudentMenuCommand.LoadAll(
                        studentId = studentId,
                        dateEpochMs = recordTime.toEpochMilliseconds(),
                        planId = MealPlanTemplateId("plan-week-1"),
                    ),
                ),
            ).toList().single(),
        )
        assertNull(loaded.diaryError)
        assertEquals(1, loaded.diaryRecords.size)
        assertEquals(studentId, diaryRepo.lastGetRangeTargetUserId)

        val granted = assertIs<CoachStudentMenuEvent.Internal.AccessGranted>(
            handler.handle(
                flowOf(
                    CoachStudentMenuCommand.GrantAccess(
                        studentId = studentId,
                        planId = "plan-week-1",
                        grantSource = GrantSource.COACH_GRANT,
                        expiresInDays = 7,
                    ),
                ),
            ).toList().single(),
        )
        assertEquals(GrantTargetType.MEAL_PLAN, granted.grant.targetType)
        assertEquals(1, coachingRepo.grants.size)

        val revoked = assertIs<CoachStudentMenuEvent.Internal.AccessRevoked>(
            handler.handle(flowOf(CoachStudentMenuCommand.RevokeAccess(granted.grant.id))).toList().single(),
        )
        assertEquals(granted.grant.id, revoked.grantId)
        assertTrue(coachingRepo.grants.isEmpty())
    }
}
