package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.presentation.formatters.DateFormatter

class CoachStudentMenuUpdate : DslUpdate<CoachStudentMenuState, CoachStudentMenuEvent, CoachStudentMenuCommand, CoachStudentMenuNews>() {

    override fun NextBuilder<CoachStudentMenuState, CoachStudentMenuCommand, CoachStudentMenuNews>.update(
        event: CoachStudentMenuEvent,
    ) {
        when (event) {
            is CoachStudentMenuEvent.Ui.Load -> {
                val planId = state.selectedPlanIdInput.trim().takeIf { it.isNotEmpty() }?.let(::MealPlanTemplateId)
                state {
                    copy(
                        studentId = event.studentId,
                        dateEpochMs = event.dateEpochMs,
                        content = CoachStudentMenuState.Content.Loading,
                    )
                }
                commands(CoachStudentMenuCommand.LoadAll(event.studentId, event.dateEpochMs, planId))
            }

            CoachStudentMenuEvent.Ui.Retry -> {
                val planId = state.selectedPlanIdInput.trim().takeIf { it.isNotEmpty() }?.let(::MealPlanTemplateId)
                state { copy(content = CoachStudentMenuState.Content.Loading) }
                commands(CoachStudentMenuCommand.LoadAll(state.studentId, state.dateEpochMs, planId))
            }

            CoachStudentMenuEvent.Ui.PreviousDayClicked -> {
                val prev = DateFormatter.shiftDaysEpochMs(state.dateEpochMs, -1)
                state { copy(dateEpochMs = prev) }
                commands(CoachStudentMenuCommand.LoadDiary(state.studentId, prev))
            }

            CoachStudentMenuEvent.Ui.NextDayClicked -> {
                val next = DateFormatter.shiftDaysEpochMs(state.dateEpochMs, 1)
                state { copy(dateEpochMs = next) }
                commands(CoachStudentMenuCommand.LoadDiary(state.studentId, next))
            }

            is CoachStudentMenuEvent.Ui.PlanIdChanged -> {
                state { copy(selectedPlanIdInput = event.value) }
            }

            is CoachStudentMenuEvent.Ui.PlanSelected -> {
                val matched = state.availablePlans.firstOrNull { it.id == event.planId }
                state {
                    copy(
                        selectedPlanIdInput = event.planId.raw,
                        selectedPlan = matched ?: state.selectedPlan,
                    )
                }
                commands(CoachStudentMenuCommand.LoadPlan(event.planId))
            }

            is CoachStudentMenuEvent.Ui.GrantSourceSelected -> {
                state { copy(grantSource = event.source) }
            }

            is CoachStudentMenuEvent.Ui.ExpiresDaysChanged -> {
                val digits = event.value.filter(Char::isDigit).take(4)
                state { copy(expiresDaysInput = digits) }
            }

            CoachStudentMenuEvent.Ui.GrantAccessClicked -> {
                val planId = state.selectedPlanIdInput.trim()
                if (planId.isEmpty()) {
                    news(CoachStudentMenuNews.ShowSnackbar("Выберите план питания или введите planId"))
                    return
                }
                if (state.isSubmittingGrant) return
                val expiresDays = state.expiresDaysInput.toIntOrNull()?.takeIf { it > 0 }
                state { copy(isSubmittingGrant = true) }
                commands(
                    CoachStudentMenuCommand.GrantAccess(
                        studentId = state.studentId,
                        planId = planId,
                        grantSource = state.grantSource,
                        expiresInDays = expiresDays,
                    ),
                )
            }

            is CoachStudentMenuEvent.Ui.RevokeAccessClicked -> {
                commands(CoachStudentMenuCommand.RevokeAccess(event.grantId))
            }

            CoachStudentMenuEvent.Ui.OpenMealPlansClicked -> {
                news(CoachStudentMenuNews.NavigateToMealPlans)
            }

            is CoachStudentMenuEvent.Internal.Loaded -> {
                val firstPlanId = state.selectedPlanIdInput.ifBlank {
                    event.selectedPlan?.id?.raw ?: event.availablePlans.firstOrNull()?.id?.raw.orEmpty()
                }
                state {
                    copy(
                        selectedPlanIdInput = firstPlanId,
                        availablePlans = event.availablePlans,
                        selectedPlan = event.selectedPlan ?: event.availablePlans.firstOrNull { it.id.raw == firstPlanId },
                        diaryRecords = event.diaryRecords,
                        diaryError = event.diaryError,
                        content = CoachStudentMenuState.Content.Loaded,
                    )
                }
            }

            is CoachStudentMenuEvent.Internal.DiaryLoaded -> {
                state {
                    copy(
                        diaryRecords = event.records,
                        diaryError = event.error,
                    )
                }
            }

            is CoachStudentMenuEvent.Internal.PlanLoaded -> {
                state { copy(selectedPlan = event.plan) }
            }

            is CoachStudentMenuEvent.Internal.PlanLoadFailed -> {
                news(CoachStudentMenuNews.ShowSnackbar(event.error.message ?: "Не удалось загрузить план питания"))
            }

            is CoachStudentMenuEvent.Internal.LoadFailed -> {
                state {
                    copy(
                        content = CoachStudentMenuState.Content.Error(
                            event.error.message ?: "Не удалось загрузить данные ученика",
                        ),
                    )
                }
            }

            is CoachStudentMenuEvent.Internal.AccessGranted -> {
                val updated = listOf(event.grant) + state.issuedGrants.filterNot { it.id == event.grant.id }
                state {
                    copy(
                        isSubmittingGrant = false,
                        issuedGrants = updated,
                    )
                }
                news(CoachStudentMenuNews.ShowSnackbar("Доступ выдан (${event.grant.id.raw})"))
            }

            is CoachStudentMenuEvent.Internal.GrantFailed -> {
                state { copy(isSubmittingGrant = false) }
                news(CoachStudentMenuNews.ShowSnackbar(event.error.message ?: "Не удалось выдать доступ"))
            }

            is CoachStudentMenuEvent.Internal.AccessRevoked -> {
                state { copy(issuedGrants = issuedGrants.filterNot { it.id == event.grantId }) }
                news(CoachStudentMenuNews.ShowSnackbar("Доступ отозван"))
            }

            is CoachStudentMenuEvent.Internal.RevokeFailed -> {
                news(CoachStudentMenuNews.ShowSnackbar(event.error.message ?: "Не удалось отозвать доступ"))
            }
        }
    }
}
