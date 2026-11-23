package dev.andre.nutritionist.android.ui.screens.catalog

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.CardDarkSurface
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.catalog.MealPlanCardUiModel

/**
 * Карточка блюда / рациона в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/Экран рационы (все блюда).png и fitapp_5/code.html.
 */
@Composable
fun MealPlanCard(
    plan: MealPlanCardUiModel,
    onPlanClick: (MealPlanId) -> Unit,
    onToggleFavorite: (MealPlanId) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Вычисляем изображение/эмодзи и время приготовления на основе названия/тегов
    val (dishEmoji, prepTime) = when {
        plan.title.contains("Овсянк", ignoreCase = true) -> "🥣" to "10 мин"
        plan.title.contains("Омлет", ignoreCase = true) || plan.title.contains("Яич", ignoreCase = true) -> "🍳" to "15 мин"
        plan.title.contains("Творог", ignoreCase = true) || plan.title.contains("Боул", ignoreCase = true) -> "🫐" to "5 мин"
        plan.title.contains("Лосось", ignoreCase = true) || plan.title.contains("Рыб", ignoreCase = true) -> "🐟" to "20 мин"
        plan.title.contains("Куриц", ignoreCase = true) || plan.title.contains("Филе", ignoreCase = true) -> "🍗" to "25 мин"
        else -> "🥗" to "15 мин"
    }

    // Вычисляем процент от суточной нормы калорий (базово 2000 ккал)
    val calNum = Regex("\\d+").find(plan.caloriesBadge)?.value?.toIntOrNull() ?: 350
    val dailyPercentage = (calNum * 100 / 2000).coerceIn(5, 50)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlanClick(MealPlanId(plan.id)) }
            .testTag("catalog_plan_card_${plan.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
        border = BorderStroke(1.dp, GlassBorderLevel1),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Верхняя часть: Название, параметры КБЖУ слева и круглая фотография блюда справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Заголовок + иконка сердечка
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = { onToggleFavorite(MealPlanId(plan.id)) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("catalog_favorite_button_${plan.id}"),
                        ) {
                            Icon(
                                imageVector = if (plan.isFavorite) FitIcons.HeartFilled else FitIcons.Heart,
                                contentDescription = if (plan.isFavorite) "В избранном" else "В избранное",
                                tint = if (plan.isFavorite) EmeraldAccent else TextMidEmphasis,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Строка 1: Калории • Цена • Время приготовления
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = plan.caloriesBadge,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent,
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(TextMidEmphasis.copy(alpha = 0.6f)),
                        )
                        Text(
                            text = plan.budgetBadge,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFCBD5E1),
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(TextMidEmphasis.copy(alpha = 0.6f)),
                        )
                        Text(
                            text = prepTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMidEmphasis,
                        )
                    }

                    // Строка 2: БЖУ
                    Text(
                        text = plan.macroSummaryText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMidEmphasis,
                    )
                }

                // Круглый фото-аватар блюда (Zero-G Luxury circular dish badge)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E2638), Color(0xFF131722)),
                            ),
                        )
                        .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.12f)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dishEmoji,
                        fontSize = 32.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Интегрированная строка доли в суточном балансе
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F121B),
                border = BorderStroke(1.dp, GlassBorderLevel2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent),
                        )
                        Text(
                            text = "Доля в дневном балансе:",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMidEmphasis,
                        )
                    }
                    Text(
                        text = "$dailyPercentage% от суточной нормы",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = EmeraldAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Нижняя строка: Теги слева и кнопка "В избранное" / "Выбрать" справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Теги
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val displayTags = if (plan.tags.isNotEmpty()) {
                        plan.tags.take(3)
                    } else {
                        listOf(plan.category, "ПП")
                    }

                    displayTags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(Spacing.pill),
                            color = Color(0xFF161A25),
                            border = BorderStroke(1.dp, GlassBorderLevel1),
                        ) {
                            Text(
                                text = if (tag.startsWith("#")) tag else "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                ),
                                color = if (tag.contains("ПП", ignoreCase = true) || tag.contains("Баланс", ignoreCase = true)) EmeraldAccent else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Кнопка действия "В избранное" / "В избранном"
                Surface(
                    shape = RoundedCornerShape(Spacing.pill),
                    color = if (plan.isFavorite) Color(0xFF0E281D) else Color(0xFF161A25),
                    border = BorderStroke(
                        1.dp,
                        if (plan.isFavorite) EmeraldAccent.copy(alpha = 0.5f) else GlassBorderLevel1,
                    ),
                    modifier = Modifier
                        .clickable { onPlanClick(MealPlanId(plan.id)) }
                        .testTag("catalog_select_plan_button_${plan.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (plan.isFavorite) FitIcons.BookmarkFilled else FitIcons.Bookmark,
                            contentDescription = null,
                            tint = if (plan.isFavorite) EmeraldAccent else Color(0xFFCBD5E1),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = if (plan.isFavorite) "В избранном" else "В избранное",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = if (plan.isFavorite) EmeraldAccent else Color(0xFFCBD5E1),
                        )
                    }
                }
            }
        }
    }
}
