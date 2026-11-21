package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.airecipe.AiRecipePromptUiModel

/**
 * Контент экрана настройки генерации рецепта с помощью AI.
 */
@Composable
fun AiRecipeConfiguringContent(
    prompt: AiRecipePromptUiModel,
    onQueryChanged: (String) -> Unit,
    onCaloriesChanged: (Int) -> Unit,
    onBudgetChanged: (Int) -> Unit,
    onMealTypeSelected: (MealType) -> Unit,
    onDietaryPreferenceSelected: (String) -> Unit,
    onGenerateClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caloriesPresets = listOf(350, 500, 650, 800, 1000)
    val budgetPresets = listOf(120, 200, 300, 450)
    val mealTypes = listOf(
        MealType.BREAKFAST to "Завтрак",
        MealType.LUNCH to "Обед",
        MealType.DINNER to "Ужин",
        MealType.SNACK to "Перекус",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        // Hero Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.cardRadius),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    Icon(
                        imageVector = FitIcons.Sparkles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "AI Шеф-нутрициолог",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Text(
                    text = "Составит сбалансированный рецепт под ваши калории, бюджет и предпочтения за секунды.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Query Input
        Column {
            Text(
                text = "Пожелания к блюду",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.extraSmall))
            OutlinedTextField(
                value = prompt.query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = "Например: легкий ужин с рыбой или сырники",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.buttonRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedBorderColor = GlassBorderLevel1,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_recipe_query_input"),
            )
        }

        // Meal Type Selector
        Column {
            Text(
                text = "Приём пищи",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_recipe_meal_types_row"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                mealTypes.forEach { (type, title) ->
                    val isSelected = prompt.mealType == type
                    Surface(
                        shape = RoundedCornerShape(Spacing.buttonRadius),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onMealTypeSelected(type) }
                            .testTag("ai_recipe_meal_type_${type.name}"),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        )
                    }
                }
            }
        }

        // Calories Target Selector
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Целевая калорийность",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = prompt.targetCaloriesText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.small))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_recipe_calories_row"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(caloriesPresets) { cal ->
                    val isSelected = prompt.targetCalories == cal
                    Surface(
                        shape = RoundedCornerShape(Spacing.buttonRadius),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onCaloriesChanged(cal) }
                            .testTag("ai_recipe_cal_$cal"),
                    ) {
                        Text(
                            text = "$cal ккал",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        )
                    }
                }
            }
        }

        // Budget Target Selector
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Бюджет на порцию",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = prompt.targetBudgetText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.small))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_recipe_budget_row"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(budgetPresets) { rub ->
                    val isSelected = prompt.targetBudgetRub == rub
                    Surface(
                        shape = RoundedCornerShape(Spacing.buttonRadius),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onBudgetChanged(rub) }
                            .testTag("ai_recipe_rub_$rub"),
                    ) {
                        Text(
                            text = "до $rub ₽",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        )
                    }
                }
            }
        }

        // Dietary Preference Chips
        Column {
            Text(
                text = "Особенности питания",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_recipe_preferences_row"),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items(prompt.availablePreferences) { pref ->
                    val isSelected = prompt.dietaryPreference == pref
                    Surface(
                        shape = RoundedCornerShape(Spacing.buttonRadius),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.tertiary else GlassBorderLevel1,
                        ),
                        modifier = Modifier
                            .clickable { onDietaryPreferenceSelected(pref) }
                            .testTag("ai_recipe_pref_$pref"),
                    ) {
                        Text(
                            text = pref,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        // CTA Generate Button
        Button(
            onClick = onGenerateClicked,
            shape = RoundedCornerShape(Spacing.buttonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.buttonHeight)
                .testTag("ai_recipe_generate_button"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Icon(
                    imageVector = FitIcons.Sparkles,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Сгенерировать рецепт Шефа",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
