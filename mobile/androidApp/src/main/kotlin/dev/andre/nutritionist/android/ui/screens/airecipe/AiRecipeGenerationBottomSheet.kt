package dev.andre.nutritionist.android.ui.screens.airecipe

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.repository.RecipeRepository
import dev.andre.nutritionist.presentation.airecipe.AiRecipeGeneratingUiModel
import kotlinx.coroutines.delay

private enum class GenerationSheetStep {
    CONFIGURING,
    GENERATING,
    SUCCESS,
}

/**
 * Всплывающее модальное окно генерации блюда с ИИ в стиле Aether Zero-G Luxury.
 * Поддерживает 3 этапа:
 * 1. Настройка параметров (screenshots/темная тема/генерация блюд с ии.png)
 * 2. Кинетическая генерация HUD (screenshots/экран при генерации.png)
 * 3. Готовый сгенерированный рецепт с добавлением в дневник
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecipeGenerationBottomSheet(
    onDismiss: () -> Unit,
    onGenerate: (query: String, budget: Int, targetCalories: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    mealType: MealType = MealType.BREAKFAST,
    targetCalories: Int = 600,
    proteinsGrams: Int = 42,
    fatsGrams: Int = 20,
    carbsGrams: Int = 65,
    initialBudget: Int = 350,
    recipeRepository: RecipeRepository? = null,
    onRecipeGeneratedAndAdded: ((Recipe) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf(GenerationSheetStep.CONFIGURING) }
    var selectedBudget by remember { mutableIntStateOf(initialBudget) }
    var fitToRemainder by remember { mutableStateOf(true) }
    var promptQuery by remember { mutableStateOf("") }

    var generatingPhaseText by remember { mutableStateOf("Анализируем нутриенты и КБЖУ...") }
    var generatingProgress by remember { mutableFloatStateOf(0.2f) }
    var generatedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var isSavedInFavorites by remember { mutableStateOf(false) }

    val budgetChips = listOf(
        200 to "до 200 ₽",
        350 to "350 ₽",
        500 to "500 ₽",
        0 to "Без лимита",
    )

    val quickChips = listOf(
        "⚡ До 15 мин",
        "🌾 Без глютена",
        "💪 Высокий белок",
        "🥬 Из простых продуктов",
    )

    // Анимация и выполнение генерации при переходе в GENERATING
    LaunchedEffect(step) {
        if (step == GenerationSheetStep.GENERATING) {
            val prompt = AiRecipePrompt(
                query = promptQuery,
                targetCalories = targetCalories,
                targetBudgetRub = selectedBudget,
                mealType = mealType,
                dietaryPreference = "Все",
            )

            // Фаза 1
            generatingPhaseText = "Анализируем нутриенты и КБЖУ..."
            generatingProgress = 0.35f
            delay(550)

            // Фаза 2
            generatingPhaseText = "Подбираем ингредиенты под бюджет..."
            generatingProgress = 0.70f
            delay(550)

            // Фаза 3
            generatingPhaseText = "Шеф составляет пошаговый рецепт..."
            generatingProgress = 0.95f
            delay(400)

            val repo = recipeRepository ?: dev.andre.nutritionist.data.repository.RecipeRepositoryImpl()
            val recipe = repo.generateRecipe(prompt).getOrThrow()

            generatingProgress = 1.0f
            generatedRecipe = recipe
            step = GenerationSheetStep.SUCCESS
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12151E),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = modifier.testTag("ai_recipe_generation_modal"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            // Светящаяся полоса Accent Glow вверху
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                EmeraldAccent.copy(alpha = 0.8f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            when (step) {
                GenerationSheetStep.CONFIGURING -> {
                    // Drag Handle & Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155).copy(alpha = 0.8f))
                                .align(Alignment.CenterHorizontally),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldAccent.copy(alpha = 0.15f))
                                        .border(BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)), RoundedCornerShape(12.dp)),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = "Генерация с ИИ",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.3).sp,
                                            ),
                                            color = Color.White,
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldAccent.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                                        ) {
                                            Text(
                                                text = "PRO",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp,
                                                ),
                                                color = EmeraldAccent,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Настройте параметры идеального приёма пищи",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextMidEmphasis,
                                    )
                                }
                            }

                            // Кнопка закрытия (X)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1B1F2B))
                                    .border(BorderStroke(1.dp, GlassBorderLevel2), CircleShape)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = FitIcons.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Scrollable Form Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Секция 1: Максимальный бюджет
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF161923),
                            border = BorderStroke(1.dp, GlassBorderLevel1),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldAccent.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = FitIcons.Wallet,
                                                contentDescription = null,
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                        Text(
                                            text = "Максимальный бюджет",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFF10131A),
                                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)),
                                    ) {
                                        Text(
                                            text = if (selectedBudget > 0) "$selectedBudget ₽" else "Без лимита",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                            ),
                                            color = EmeraldAccent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }

                                // Чипы бюджета
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    budgetChips.forEach { (amount, label) ->
                                        val isSelected = selectedBudget == amount
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) EmeraldAccent.copy(alpha = 0.18f) else Color(0xFF10131A),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) EmeraldAccent.copy(alpha = 0.5f) else GlassBorderLevel1,
                                            ),
                                            modifier = Modifier
                                                .clickable { selectedBudget = amount }
                                                .testTag("ai_modal_budget_chip_$amount"),
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp,
                                                ),
                                                color = if (isSelected) EmeraldAccent else Color(0xFFCBD5E1),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Секция 2: Баланс КБЖУ
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF161923),
                            border = BorderStroke(1.dp, GlassBorderLevel1),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF00E5FF).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = FitIcons.Lightning,
                                                contentDescription = null,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                        Text(
                                            text = "Баланс КБЖУ",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                    }

                                    // Переключатель "Вписать в остаток"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = "Вписать в остаток",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = EmeraldAccent,
                                        )
                                        Switch(
                                            checked = fitToRemainder,
                                            onCheckedChange = { fitToRemainder = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF090B10),
                                                checkedTrackColor = EmeraldAccent,
                                                uncheckedThumbColor = Color(0xFF94A3B8),
                                                uncheckedTrackColor = Color(0xFF1E2638),
                                            ),
                                            modifier = Modifier.height(24.dp),
                                        )
                                    }
                                }

                                // Планка с 4 макронутриентами
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10131A),
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Ккал",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF64748B),
                                            )
                                            Text(
                                                text = "$targetCalories",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                                color = EmeraldAccent,
                                            )
                                        }
                                        Box(modifier = Modifier.size(width = 1.dp, height = 20.dp).background(Color(0xFF272B3A)))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Белки",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF64748B),
                                            )
                                            Text(
                                                text = "$proteinsGrams г",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE2E8F0),
                                            )
                                        }
                                        Box(modifier = Modifier.size(width = 1.dp, height = 20.dp).background(Color(0xFF272B3A)))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Жиры",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF64748B),
                                            )
                                            Text(
                                                text = "$fatsGrams г",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE2E8F0),
                                            )
                                        }
                                        Box(modifier = Modifier.size(width = 1.dp, height = 20.dp).background(Color(0xFF272B3A)))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Углеводы",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF64748B),
                                            )
                                            Text(
                                                text = "$carbsGrams г",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE2E8F0),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Секция 3: Пожелания и ингредиенты
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF161923),
                            border = BorderStroke(1.dp, GlassBorderLevel1),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFB020).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = FitIcons.Edit,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB020),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Text(
                                            text = "Пожелания и ингредиенты",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                    }
                                    Text(
                                        text = "ИИ учтет все детали",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF64748B),
                                    )
                                }

                                // Многострочное текстовое поле
                                OutlinedTextField(
                                    value = promptQuery,
                                    onValueChange = { promptQuery = it },
                                    placeholder = {
                                        Text(
                                            text = "Сытный утренний приём без лактозы, яйца, тост или авокадо...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B),
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldAccent.copy(alpha = 0.5f),
                                        unfocusedBorderColor = GlassBorderLevel2,
                                        focusedContainerColor = Color(0xFF10131A),
                                        unfocusedContainerColor = Color(0xFF10131A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_modal_query_input"),
                                )

                                // Быстрые теги пожеланий
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    quickChips.forEach { chipText ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1C2130),
                                            border = BorderStroke(1.dp, GlassBorderLevel1),
                                            modifier = Modifier
                                                .clickable {
                                                    promptQuery = if (promptQuery.isBlank()) chipText else "$promptQuery, $chipText"
                                                },
                                        ) {
                                            Text(
                                                text = chipText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFFCBD5E1),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions Footer: Главная кнопка и Отмена
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                step = GenerationSheetStep.GENERATING
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color(0xFF090B10),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("ai_modal_generate_button"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = FitIcons.Sparkles,
                                    contentDescription = null,
                                    tint = Color(0xFF090B10),
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "✦ Сгенерировать приём пищи ✨",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.2).sp,
                                    ),
                                )
                            }
                        }

                        Text(
                            text = "Отмена",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color(0xFF94A3B8),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 4.dp),
                        )
                    }
                }

                GenerationSheetStep.GENERATING -> {
                    // Кинетическая генерация HUD (полноэкранный HUD в модалке)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp),
                    ) {
                        AiRecipeGeneratingContent(
                            generating = AiRecipeGeneratingUiModel(
                                phaseText = generatingPhaseText,
                                progress = generatingProgress,
                                progressPercentText = "${(generatingProgress * 100).toInt()}%",
                            ),
                            targetCalories = targetCalories,
                            targetBudgetRub = selectedBudget,
                            onCancel = {
                                step = GenerationSheetStep.CONFIGURING
                            },
                        )
                    }
                }

                GenerationSheetStep.SUCCESS -> {
                    val recipe = generatedRecipe
                    if (recipe != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // Header: Успешная генерация
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(text = "✓", color = EmeraldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Блюдо сгенерировано!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = EmeraldAccent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                                ) {
                                    Text(
                                        text = "${recipe.calories} ккал",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }

                            // Dish Hero Card
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF161923),
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = recipe.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = recipe.description,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color(0xFFCBD5E1),
                                    )

                                    // Chips row: Cost, Time
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E2433),
                                            border = BorderStroke(1.dp, GlassBorderLevel1),
                                        ) {
                                            Text(
                                                text = "💰 ~${recipe.estimatedCostRub} ₽",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFFB020),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E2433),
                                            border = BorderStroke(1.dp, GlassBorderLevel1),
                                        ) {
                                            Text(
                                                text = "⏱ ${recipe.cookingTimeMinutes} мин",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                    }

                                    // Macro summary bar
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10131A),
                                        border = BorderStroke(1.dp, GlassBorderLevel1),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "Б: ${recipe.proteins}г",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE2E8F0),
                                            )
                                            Text(
                                                text = "Ж: ${recipe.fats}г",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE2E8F0),
                                            )
                                            Text(
                                                text = "У: ${recipe.carbs}г",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFE2E8F0),
                                            )
                                        }
                                    }
                                }
                            }

                            // Ingredients Preview
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF161923),
                                border = BorderStroke(1.dp, GlassBorderLevel1),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Ингредиенты:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    recipe.ingredients.take(4).forEach { ing ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "• ${ing.name}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = Color(0xFFCBD5E1),
                                            )
                                            Text(
                                                text = ing.amount,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                                color = EmeraldAccent,
                                            )
                                        }
                                    }
                                }
                            }

                            // CTA Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        onRecipeGeneratedAndAdded?.invoke(recipe)
                                            ?: onGenerate(promptQuery, selectedBudget, targetCalories)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldAccent,
                                        contentColor = Color(0xFF090B10),
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("ai_modal_add_diary_button"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = FitIcons.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF090B10),
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = "Добавить в дневник питания",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                            ),
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        isSavedInFavorites = !isSavedInFavorites
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (isSavedInFavorites) EmeraldAccent else GlassBorderLevel2),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF161923),
                                        contentColor = if (isSavedInFavorites) EmeraldAccent else Color(0xFFCBD5E1),
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                ) {
                                    Text(
                                        text = if (isSavedInFavorites) "✓ В избранном" else "☆ Сохранить в избранное",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                }

                                Text(
                                    text = "Попробовать другой вариант",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .clickable {
                                            step = GenerationSheetStep.CONFIGURING
                                        }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
