package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class DiaryUiStateMapper(
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : UiStateMapper<DiaryState, DiaryUiState> {

    override fun map(state: DiaryState): DiaryUiState {
        val uiContent = when (val c = state.content) {
            DiaryState.Content.Loading -> DiaryUiState.Content.Loading
            DiaryState.Content.Success -> DiaryUiState.Content.Success
            DiaryState.Content.Empty -> DiaryUiState.Content.Empty
            is DiaryState.Content.Error -> DiaryUiState.Content.Error(c.message)
        }
        val dateLabel = if (state.selectedDateEpochMs > 0L) {
            DateFormatter.formatDayHeader(
                epochMs = state.selectedDateEpochMs,
                nowEpochMs = nowProvider(),
                timeZone = timeZone,
            )
        } else {
            ""
        }
        return DiaryUiState(
            selectedDateLabel = dateLabel,
            items = state.items,
            dailySummary = state.dailySummary,
            content = uiContent,
        )
    }
}
