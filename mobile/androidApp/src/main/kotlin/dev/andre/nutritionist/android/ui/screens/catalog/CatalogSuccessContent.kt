package dev.andre.nutritionist.android.ui.screens.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.screens.EmeraldAccent
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.catalog.CatalogSuccessUiModel

/**
 * Контент экрана блюд и рационов в стиле Aether Zero-G Luxury.
 * Полностью соответствует screenshots/темная тема/Экран рационы (все блюда).png и fitapp_5/code.html.
 */
@Composable
fun CatalogSuccessContent(
    data: CatalogSuccessUiModel,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPlanSelected: (MealPlanId) -> Unit,
    onToggleFavorite: (MealPlanId) -> Unit,
    onGenerateAiPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Вкладка: 0 - Все блюда, 1 - Избранное
    var selectedTab by remember { mutableStateOf(0) }

    // Используем data.plans напрямую. Если список пуст, отображаем плейсхолдер catalog_empty_plans
    val allDishes = data.plans

    // Фильтрация по поиску и вкладкам
    val filteredDishes = allDishes.filter { plan ->
        val matchesTab = if (selectedTab == 1) plan.isFavorite else true
        val matchesQuery = if (data.searchQuery.isNotBlank()) {
            plan.title.contains(data.searchQuery, ignoreCase = true) ||
                plan.tags.any { it.contains(data.searchQuery, ignoreCase = true) }
        } else {
            true
        }
        matchesTab && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.medium),
    ) {
        // 1. Поисковая строка (закреплена вверху, не скроллится)
        OutlinedTextField(
            value = data.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = "Поиск блюд, калорий или продуктов...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = FitIcons.Search,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    if (data.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChanged("") },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = FitIcons.Close,
                                contentDescription = "Очистить",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF191E2B))
                            .border(BorderStroke(1.dp, GlassBorderLevel2), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FitIcons.Filter,
                            contentDescription = "Фильтры",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldAccent.copy(alpha = 0.7f),
                unfocusedBorderColor = GlassBorderLevel2,
                focusedContainerColor = Color(0xFF131622),
                unfocusedContainerColor = Color(0xFF131622),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("catalog_search_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Две переключаемые категории: "• Все блюда" и "❤️ Избранное" (закреплены вверху)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Кнопка "• Все блюда"
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { selectedTab = 0 },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTab == 0) EmeraldAccent else Color(0xFF141824),
                border = BorderStroke(
                    1.dp,
                    if (selectedTab == 0) EmeraldAccent else GlassBorderLevel1,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (selectedTab == 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF090B10)),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                    Text(
                        text = "Все блюда",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                        ),
                        color = if (selectedTab == 0) Color(0xFF090B10) else Color(0xFFCBD5E1),
                    )
                }
            }

            // Кнопка "❤️ Избранное"
            val favoritesCount = allDishes.count { it.isFavorite }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { selectedTab = 1 },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTab == 1) EmeraldAccent else Color(0xFF141824),
                border = BorderStroke(
                    1.dp,
                    if (selectedTab == 1) EmeraldAccent else GlassBorderLevel1,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.HeartFilled,
                        contentDescription = null,
                        tint = if (selectedTab == 1) Color(0xFF090B10) else Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Избранное",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.SemiBold,
                        ),
                        color = if (selectedTab == 1) Color(0xFF090B10) else Color(0xFFCBD5E1),
                    )
                    if (favoritesCount > 0) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selectedTab == 1) Color(0xFF090B10).copy(alpha = 0.2f) else Color(0xFF1E2638))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "$favoritesCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                ),
                                color = if (selectedTab == 1) Color(0xFF090B10) else Color.White,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Скроллируемая лента блюд и категорий
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.large)
                .testTag("catalog_content_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Подкатегории для быстрой фильтрации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("catalog_categories_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                data.categories.forEach { category ->
                    Surface(
                        shape = RoundedCornerShape(Spacing.pill),
                        color = if (category.isSelected) EmeraldAccent.copy(alpha = 0.2f) else Color(0xFF141824),
                        border = BorderStroke(
                            1.dp,
                            if (category.isSelected) EmeraldAccent else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onCategorySelected(category.title) }
                            .testTag("catalog_category_chip_${category.title}"),
                    ) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (category.isSelected) EmeraldAccent else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // AI Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_ai_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0E281D), Color(0xFF091A14)),
                            ),
                        )
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldAccent.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = FitIcons.Sparkles,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Персональный AI-рацион",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Сгенерируйте блюдо под ваш бюджет и цели",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextMidEmphasis,
                                )
                            }
                        }

                        Button(
                            onClick = onGenerateAiPlanClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color(0xFF090B10),
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("catalog_generate_ai_button"),
                        ) {
                            Text(
                                text = "ИИ Шеф",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }

            // Список карточек блюд
            if (filteredDishes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.large)
                        .testTag("catalog_empty_plans"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "🥣",
                            fontSize = 36.sp,
                        )
                        Text(
                            text = "Рационы не найдены",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "Попробуйте изменить запрос или сбросить фильтр",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMidEmphasis,
                        )
                    }
                }
            } else {
                filteredDishes.forEach { plan ->
                    MealPlanCard(
                        plan = plan,
                        onPlanClick = onPlanSelected,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }
}
