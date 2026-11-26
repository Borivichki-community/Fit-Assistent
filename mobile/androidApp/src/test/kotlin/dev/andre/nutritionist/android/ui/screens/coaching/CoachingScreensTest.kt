package dev.andre.nutritionist.android.ui.screens.coaching

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.coaching.CoachStudentLinkUiModel
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuEvent
import dev.andre.nutritionist.presentation.coaching.CoachStudentMenuUiState
import dev.andre.nutritionist.presentation.coaching.CoachStudentsEvent
import dev.andre.nutritionist.presentation.coaching.CoachStudentsUiState
import dev.andre.nutritionist.presentation.coaching.StudentCoachInviteUiModel
import dev.andre.nutritionist.presentation.coaching.StudentCoachingEvent
import dev.andre.nutritionist.presentation.coaching.StudentCoachingUiState
import dev.andre.nutritionist.presentation.mealplans.MealPlanCardUiModel
import dev.andre.nutritionist.presentation.mealplans.MealPlansEvent
import dev.andre.nutritionist.presentation.mealplans.MealPlansUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru-rRU-w411dp-h915dp")
class CoachingScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coachStudentsScreenRendersLinksAndDispatchesInvite() {
        val events = mutableListOf<CoachStudentsEvent.Ui>()
        val state = CoachStudentsUiState(
            studentIdInput = "student-10",
            canViewDiary = true,
            content = CoachStudentsUiState.Content.Success(
                links = listOf(
                    CoachStudentLinkUiModel(
                        linkId = "l1",
                        studentId = UserId("student-1"),
                        studentLabel = "student-1",
                        status = CoachLinkStatus.ACTIVE,
                        statusLabel = "ACTIVE",
                        canViewDiary = true,
                        diaryAccessLabel = "Доступ к дневнику: Да",
                        canOpenDiary = true,
                    ),
                ),
            ),
        )

        composeRule.setContent {
            CoachStudentsScreen(
                state = state,
                onEvent = { events.add(it) },
                onBackClick = {},
            )
        }

        composeRule.onNodeWithText("student-1").assertIsDisplayed()
        composeRule.onNodeWithText("Открыть дневник").performClick()
        composeRule.onNodeWithText("Пригласить ученика").performClick()
        assertTrue(events.contains(CoachStudentsEvent.Ui.OpenDiaryClicked(UserId("student-1"))))
        assertTrue(events.contains(CoachStudentsEvent.Ui.InviteClicked))
    }

    @Test
    fun studentCoachingScreenAcceptsAndRevokesCoach() {
        val events = mutableListOf<StudentCoachingEvent.Ui>()
        val state = StudentCoachingUiState(
            content = StudentCoachingUiState.Content.Success(
                pendingInvites = listOf(
                    StudentCoachInviteUiModel(
                        linkId = CoachLinkId("link-pending"),
                        coachLabel = "Тренер: coach-1",
                        status = CoachLinkStatus.PENDING,
                        canViewDiary = true,
                        diaryAccessLabel = "Доступ к дневнику разрешён",
                        isBusy = false,
                    ),
                ),
                activeCoaches = listOf(
                    StudentCoachInviteUiModel(
                        linkId = CoachLinkId("link-active"),
                        coachLabel = "Тренер: coach-2",
                        status = CoachLinkStatus.ACTIVE,
                        canViewDiary = true,
                        diaryAccessLabel = "Доступ к дневнику разрешён",
                        isBusy = false,
                    ),
                ),
                availablePlans = emptyList(),
            ),
        )

        composeRule.setContent {
            StudentCoachingScreen(
                state = state,
                onEvent = { events.add(it) },
                onBackClick = {},
            )
        }

        composeRule.onNodeWithText("Принять").performClick()
        composeRule.onNodeWithText("Отвязать тренера").performClick()
        assertTrue(events.contains(StudentCoachingEvent.Ui.AcceptInviteClicked(CoachLinkId("link-pending"))))
        assertTrue(events.contains(StudentCoachingEvent.Ui.RevokeCoachClicked(CoachLinkId("link-active"))))
    }

    @Test
    fun coachStudentMenuScreenDispatchesGrantAccess() {
        val menuEvents = mutableListOf<CoachStudentMenuEvent.Ui>()
        val menuState = CoachStudentMenuUiState(
            studentIdLabel = "student-1",
            dateLabel = "Сегодня",
            planIdInput = "plan-1",
            content = CoachStudentMenuUiState.Content.Success(
                selectedPlanTitle = "План 7 дней",
                selectedPlanSummary = "7 дн. • FREE",
                availablePlans = emptyList(),
                factSummaryLabel = "Факт: 400 ккал",
                diaryError = null,
                diaryEntries = emptyList(),
                issuedGrants = emptyList(),
            ),
        )
        composeRule.setContent {
            CoachStudentMenuScreen(
                state = menuState,
                onEvent = { menuEvents.add(it) },
                onBackClick = {},
            )
        }
        composeRule.onNodeWithText("Выдать доступ к меню").performClick()
        assertTrue(menuEvents.contains(CoachStudentMenuEvent.Ui.GrantAccessClicked))
    }

    @Test
    fun mealPlansScreenDispatchesPublishAndApplyDay() {
        val planEvents = mutableListOf<MealPlansEvent.Ui>()
        val planState = MealPlansUiState(
            isCoach = true,
            titleInput = "Новое меню",
            content = MealPlansUiState.Content.Success(
                selectedPlanTitle = null,
                selectedPlanDetail = null,
                plans = listOf(
                    MealPlanCardUiModel(
                        id = MealPlanTemplateId("plan-1"),
                        title = "Меню на неделю",
                        metaLabel = "7 дн. • FREE • PRIVATE",
                        idLabel = "planId: plan-1",
                        isPublic = false,
                        hasItemsForDayOne = true,
                    ),
                ),
                recipes = emptyList(),
            ),
        )
        composeRule.setContent {
            MealPlansAndPublishScreen(
                state = planState,
                onEvent = { planEvents.add(it) },
                onBackClick = {},
            )
        }
        composeRule.onNodeWithText("Опубликовать в общий доступ").performClick()
        composeRule.onNodeWithText("Применить День 1 в дневник").performClick()
        assertTrue(planEvents.contains(MealPlansEvent.Ui.PublishPlanClicked(MealPlanTemplateId("plan-1"))))
        assertTrue(planEvents.contains(MealPlansEvent.Ui.ApplyPlanDayClicked(MealPlanTemplateId("plan-1"), 1)))
    }
}
