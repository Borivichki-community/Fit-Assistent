package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.Spacing

/**
 * Карточка недельного календаря (в точности по скриншоту главный экран.jpg).
 */
@Composable
fun CalendarCard(
    month: String,
    selectedDay: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(Spacing.cardRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
        ) {
            // Заголовок месяца и стрелки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    CircleArrowButton(
                        direction = "<",
                        contentDescription = stringResource(R.string.calendar_prev),
                        onClick = {},
                    )
                    CircleArrowButton(
                        direction = ">",
                        contentDescription = stringResource(R.string.calendar_next),
                        onClick = {},
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Дни недели: Пн Вт Ср Чт Пт Сб Вс / Mon Tue Wed...
            val daysOfWeek = listOf(
                stringResource(R.string.day_mon),
                stringResource(R.string.day_tue),
                stringResource(R.string.day_wed),
                stringResource(R.string.day_thu),
                stringResource(R.string.day_fri),
                stringResource(R.string.day_sat),
                stringResource(R.string.day_sun),
            )
            val dates = listOf("02", "03", "04", "05", "06", "07", "08")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Числа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dates.forEach { date ->
                    val isSelected = date.toIntOrNull() == selectedDay
                    DateCircleItem(date = date, isSelected = isSelected)
                }
            }
        }
    }
}
