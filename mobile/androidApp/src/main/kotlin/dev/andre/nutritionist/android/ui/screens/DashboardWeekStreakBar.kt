package dev.andre.nutritionist.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.presentation.dashboard.DayOfWeekUiModel

/**
 * Недельный календарь и стрик в стиле Aether Zero-G Luxury.
 * Соответствует скриншоту screenshots/темная тема/дневник.png и fitapp_1/code.html.
 */
@Composable
fun DashboardWeekStreakBar(
    selectedDateLabel: String,
    streakDays: Int,
    weekDays: List<DayOfWeekUiModel>,
    modifier: Modifier = Modifier,
    monthLabel: String = "Ноябрь 2026",
    weekNumberLabel: String = "Неделя 45",
    onDayClick: (DayOfWeekUiModel) -> Unit = {},
    onPrevWeekClick: () -> Unit = {},
    onNextWeekClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDarkSurface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Верхняя строка: Месяц + Неделя 45 + Серия + Стрелки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0E281D),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            text = weekNumberLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Кнопки навигации по неделям < >
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B1F2B))
                            .clickable { onPrevWeekClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.ChevronLeft,
                            contentDescription = "Предыдущая неделя",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B1F2B))
                            .clickable { onNextWeekClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.ChevronRight,
                            contentDescription = "Следующая неделя",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Строка со стриком и статусом даты (для тестов и информативности)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedDateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMidEmphasis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FitIcons.Flame,
                        contentDescription = null,
                        tint = Color(0xFFFF9F1C),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.streak_full_format, streakDays),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF9F1C),
                    )
                }
            }

            // Карусель 7 дней недели: Пн 02, Вт 03, Ср 04, Чт 05, Пт 06, Сб 07 (Active), Вс 08
            val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            val dayNumbers = listOf("02", "03", "04", "05", "06", "07", "08")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dayNames.forEachIndexed { index, defaultName ->
                    val model = weekDays.getOrNull(index)
                    val name = model?.dayName ?: defaultName
                    val number = model?.dayNumber?.ifBlank { null } ?: dayNumbers.getOrElse(index) { "01" }
                    val isActive = model?.isSelected ?: (index == 5)
                    val hasCompletedDot = model?.isCompleted ?: (index == 4 || isActive)

                    if (isActive) {
                        // Активный выбранный день (белая пилюля)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier
                                .width(42.dp)
                                .clickable {
                                    val target = model ?: DayOfWeekUiModel(name, isSelected = true)
                                    onDayClick(target)
                                },
                            border = BorderStroke(1.dp, EmeraldAccent),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF334155),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = number,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldAccent),
                                )
                            }
                        }
                    } else {
                        // Обычные дни недели
                        Column(
                            modifier = Modifier
                                .width(42.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val target = model ?: DayOfWeekUiModel(name)
                                    onDayClick(target)
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = number,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            if (hasCompletedDot) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldAccent),
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
