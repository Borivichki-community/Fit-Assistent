package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeCoachingRepository
import dev.andre.nutritionist.fake.FakeMealPlanRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StudentCoachingFlowTest {

    @Test
    fun `update maps accept and revoke actions cleanly`() {
        val update = StudentCoachingUpdate()
        val linkId = CoachLinkId("link-1")
        val initial = StudentCoachingState(currentUserId = UserId("student-1"))

        val acceptResult = update.update(initial, StudentCoachingEvent.Ui.AcceptInviteClicked(linkId))
        assertEquals(listOf(StudentCoachingCommand.RespondInvite(linkId, accept = true)), acceptResult.commands)

        val revokeResult = update.update(initial, StudentCoachingEvent.Ui.RevokeCoachClicked(linkId))
        assertEquals(listOf(StudentCoachingCommand.RespondInvite(linkId, accept = false)), revokeResult.commands)

        val mapped = StudentCoachingUiStateMapper().map(initial)
        assertEquals("student-1", mapped.currentUserId)
    }

    @Test
    fun `handler loads student invites and responds to invitation`() = runTest {
        val coachingRepo = FakeCoachingRepository().apply {
            links.add(
                CoachStudentLink(
                    id = CoachLinkId("link-pending"),
                    coachId = UserId("coach-1"),
                    studentId = UserId("student-1"),
                    status = CoachLinkStatus.PENDING,
                    canViewDiary = true,
                ),
            )
        }
        val planRepo = FakeMealPlanRepository().apply {
            plans.add(
                MealPlanTemplate(
                    id = MealPlanTemplateId("plan-1"),
                    authorId = UserId("coach-1"),
                    title = "Недельное меню",
                    durationDays = 7,
                ),
            )
        }
        val handler = StudentCoachingCommandsFlowHandler(
            coachingRepository = coachingRepo,
            mealPlanRepository = planRepo,
            dispatchers = TestCoroutineDispatchers(),
        )

        val loaded = assertIs<StudentCoachingEvent.Internal.DataLoaded>(
            handler.handle(flowOf(StudentCoachingCommand.LoadData(UserId("student-1")))).toList().single(),
        )
        assertEquals(1, loaded.links.size)
        assertEquals(1, loaded.plans.size)

        val responded = assertIs<StudentCoachingEvent.Internal.InviteResponded>(
            handler.handle(
                flowOf(StudentCoachingCommand.RespondInvite(CoachLinkId("link-pending"), accept = true)),
            ).toList().single(),
        )
        assertEquals(CoachLinkStatus.ACTIVE, responded.updatedLink.status)
    }
}
