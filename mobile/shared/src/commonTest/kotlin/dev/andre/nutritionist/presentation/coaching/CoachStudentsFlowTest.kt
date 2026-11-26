package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeCoachingRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoachStudentsFlowTest {

    @Test
    fun `update validates blank student id and emits InviteStudent command when valid`() {
        val update = CoachStudentsUpdate()
        val initial = CoachStudentsState(currentUserId = UserId("coach-1"))

        val blankResult = update.update(initial, CoachStudentsEvent.Ui.InviteClicked)
        assertTrue(blankResult.commands.isEmpty())
        assertTrue(blankResult.news.firstOrNull() is CoachStudentsNews.ShowSnackbar)

        val typed = requireNotNull(update.update(initial, CoachStudentsEvent.Ui.StudentIdChanged("student-7")).state)
        val validResult = update.update(typed, CoachStudentsEvent.Ui.InviteClicked)
        assertEquals(
            listOf(
                CoachStudentsCommand.InviteStudent(
                    coachUserId = UserId("coach-1"),
                    studentId = UserId("student-7"),
                    canViewDiary = true,
                ),
            ),
            validResult.commands,
        )
    }

    @Test
    fun `handler loads coach links and invites student`() = runTest {
        val repo = FakeCoachingRepository().apply {
            lastInvitedCoachId = UserId("coach-1")
            links.add(
                CoachStudentLink(
                    id = CoachLinkId("link-existing"),
                    coachId = UserId("coach-1"),
                    studentId = UserId("student-1"),
                    status = CoachLinkStatus.ACTIVE,
                    canViewDiary = true,
                ),
            )
            links.add(
                CoachStudentLink(
                    id = CoachLinkId("link-other"),
                    coachId = UserId("coach-2"),
                    studentId = UserId("coach-1"),
                    status = CoachLinkStatus.PENDING,
                    canViewDiary = false,
                ),
            )
        }
        val handler = CoachStudentsCommandsFlowHandler(repo, TestCoroutineDispatchers())

        val loadEvents = handler.handle(flowOf(CoachStudentsCommand.LoadLinks(UserId("coach-1")))).toList()
        val loaded = assertIs<CoachStudentsEvent.Internal.LinksLoaded>(loadEvents.single())
        assertEquals(listOf(UserId("student-1")), loaded.links.map { it.studentId })

        val inviteEvents = handler.handle(
            flowOf(
                CoachStudentsCommand.InviteStudent(
                    coachUserId = UserId("coach-1"),
                    studentId = UserId("student-2"),
                    canViewDiary = true,
                ),
            ),
        ).toList()
        val invited = assertIs<CoachStudentsEvent.Internal.StudentInvited>(inviteEvents.single())
        assertEquals(UserId("student-2"), invited.link.studentId)
        assertEquals(CoachLinkStatus.PENDING, invited.link.status)
    }
}
