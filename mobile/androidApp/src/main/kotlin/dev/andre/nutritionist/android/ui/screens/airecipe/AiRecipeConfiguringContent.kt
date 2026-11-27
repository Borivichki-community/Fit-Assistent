package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent.Ui
import dev.andre.nutritionist.presentation.airecipe.AiRecipePrompt
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState.Content

private val SheetSurface = Color(0xFF12151E)
private val CardSurface = Color(0xFF161923)
private val ControlSurface = Color(0xFF10131A)
private val BudgetInputSurface = Color(0xFF10131B)
private val ChipUnselectedSurface = Color(0xFF1C2130)
private val CloseBtnSurface = Color(0xFF1B1F2B)
private val MintAccent = Color(0xFF00F5A0)
private val MintButtonEnd = Color(0xFF00D680)
private val CyanAccent = Color(0xFF22D3EE)
private val AmberAccent = Color(0xFFFBBF24)
private val MutedText = Color(0xFF94A3B8)
private val MutedDimText = Color(0xFF64748B)
private val ChipText = Color(0xFFCBD5E1)
private val HairlineWhite = Color(0x0FFFFFFF)
private val CardOutline = Color(0x0DFFFFFF)
private val InputOutline = Color(0x1AFFFFFF)

@Composable
internal fun AiRecipeConfiguringContent(
    state: Content.Configuring,
    onEvent: (Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    RecipeSettingsPanel(state = state, onEvent = onEvent, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeSettingsPanel(
    state: Content.Configuring,
    onEvent: (Ui) -> Unit,
    modifier: Modifier,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    val focusManager = LocalFocusManager.current
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val dietaryTags = listOf(
        "⚡ До 15 мин",
        "🌾 Без глютена",
        "💪 Высокий белок",
        "🥬 Из простых продуктов",
    )

    Column(
        modifier = modifier
            .widthIn(max = 480.dp)
            .heightIn(max = maxSheetHeight)
            .fillMaxWidth()
            .shadow(24.dp, sheetShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(sheetShape)
            .background(SheetSurface)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(MintAccent.copy(alpha = 0.30f), CardOutline),
                    endY = 120f,
                ),
                shape = sheetShape,
            )
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(192.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to MintAccent.copy(alpha = 0.80f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155).copy(alpha = 0.80f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp), ambientColor = MintAccent.copy(alpha = 0.3f), spotColor = MintAccent.copy(alpha = 0.3f))
                    .background(MintAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, MintAccent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FitIcons.Sparkle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintAccent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Генерация с ИИ",
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "PRO",
                        modifier = Modifier
                            .background(MintAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, MintAccent.copy(alpha = 0.30f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        color = MintAccent,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
                Text(
                    text = "Настройте параметры идеального приёма пищи",
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CloseBtnSurface)
                    .border(1.dp, CardOutline, CircleShape)
                    .clickable(role = Role.Button) {
                        focusManager.clearFocus(force = true)
                        onEvent(Ui.CancelClicked)
                    }
                    .semantics { contentDescription = "Закрыть" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FitIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MutedText,
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HairlineWhite))

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecipeSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingTitle(
                        icon = FitIcons.Wallet,
                        label = "Максимальный бюджет",
                        tint = MintAccent,
                        modifier = Modifier.weight(1f),
                    )
                    BudgetInputPill(
                        targetBudgetRub = state.prompt.targetBudgetRub,
                        budgetLabel = state.budgetLabel,
                        onBudgetChanged = { onEvent(Ui.BudgetChanged(it)) },
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.budgets.forEach { (value, label) ->
                        BudgetChip(
                            label = label,
                            selected = state.prompt.targetBudgetRub == value,
                            onClick = { onEvent(Ui.BudgetChanged(value)) },
                        )
                    }
                }
            }

            RecipeSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingTitle(
                        icon = FitIcons.Lightning,
                        label = "Баланс КБЖУ",
                        tint = CyanAccent,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Вписать в остаток",
                        color = MintAccent,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(8.dp))
                    val fitRemaining = state.prompt.fitRemaining
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 20.dp)
                            .clip(CircleShape)
                            .background(if (fitRemaining) MintAccent else Color(0xFF282D39))
                            .toggleable(fitRemaining, role = Role.Switch) { onEvent(Ui.FitRemainingChanged(it)) }
                            .semantics { contentDescription = "Вписать в остаток" }
                            .padding(2.dp),
                        contentAlignment = if (fitRemaining) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .shadow(2.dp, CircleShape)
                                .background(Color(0xFF020617), CircleShape),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ControlSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, MintAccent.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.nutrition.forEachIndexed { index, (label, value) ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(1.dp)
                                    .background(InputOutline),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = label,
                                color = MutedDimText,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                            )
                            Text(
                                text = value,
                                color = if (index == 0) MintAccent else Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            RecipeSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingTitle(
                        icon = FitIcons.Edit,
                        label = "Пожелания и ингредиенты",
                        tint = AmberAccent,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "ИИ учтёт все детали",
                        color = MutedDimText,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                    )
                }
                BasicTextField(
                    value = state.prompt.query,
                    onValueChange = { onEvent(Ui.QueryChanged(it)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    cursorBrush = SolidColor(MintAccent),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Пожелания и ингредиенты" },
                    decorationBox = { field ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ControlSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, InputOutline, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                        ) {
                            if (state.prompt.query.isEmpty()) {
                                Text(
                                    text = "Сытный утренний приём без лактозы, яйца, тост или авокадо...",
                                    color = MutedDimText,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                            }
                            field()
                        }
                    },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    dietaryTags.forEach { label ->
                        PreferenceChip(
                            label = label,
                            selected = state.prompt.dietaryPreference == label,
                            onClick = { onEvent(Ui.DietaryPreferenceSelected(label)) },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val buttonShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = buttonShape,
                        ambientColor = MintAccent.copy(alpha = 0.35f),
                        spotColor = MintAccent.copy(alpha = 0.35f),
                    )
                    .clip(buttonShape)
                    .background(Brush.horizontalGradient(listOf(MintAccent, MintButtonEnd)))
                    .clickable(role = Role.Button) {
                        focusManager.clearFocus(force = true)
                        onEvent(Ui.GenerateClicked)
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FitIcons.Sparkle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF020617),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Сгенерировать приём пищи ✨",
                    color = Color(0xFF020617),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                text = "Отмена",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) {
                        focusManager.clearFocus(force = true)
                        onEvent(Ui.CancelClicked)
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BudgetInputPill(
    targetBudgetRub: Int,
    budgetLabel: String,
    onBudgetChanged: (Int) -> Unit,
) {
    var draftDigits by remember(targetBudgetRub) {
        mutableStateOf(
            if (targetBudgetRub == AiRecipePrompt.NO_BUDGET_LIMIT) "" else targetBudgetRub.toString(),
        )
    }
    Row(
        modifier = Modifier
            .background(BudgetInputSurface, RoundedCornerShape(12.dp))
            .border(1.dp, InputOutline, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (targetBudgetRub == AiRecipePrompt.NO_BUDGET_LIMIT) {
            Text(
                text = budgetLabel,
                color = MintAccent,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        } else {
            BasicTextField(
                value = draftDigits,
                onValueChange = { raw ->
                    val filtered = raw.filter(Char::isDigit).take(6)
                    draftDigits = filtered
                    val parsed = filtered.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onBudgetChanged(parsed)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MintAccent),
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    color = MintAccent,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.End,
                ),
                modifier = Modifier
                    .widthIn(min = 28.dp, max = 64.dp)
                    .semantics { contentDescription = "Сумма бюджета" },
            )
            Text(
                text = "₽",
                color = MintAccent,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun RecipeSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, CardOutline, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun SettingTitle(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tint,
            )
        }
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BudgetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = label,
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = MintAccent.copy(alpha = 0.30f),
                        spotColor = MintAccent.copy(alpha = 0.30f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(if (selected) MintAccent.copy(alpha = 0.15f) else ChipUnselectedSurface)
            .border(
                width = 1.dp,
                color = if (selected) MintAccent.copy(alpha = 0.35f) else CardOutline,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = if (selected) MintAccent else ChipText,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
}

@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = label,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) MintAccent.copy(alpha = 0.14f) else ChipUnselectedSurface)
            .border(
                width = 1.dp,
                color = if (selected) MintAccent.copy(alpha = 0.45f) else CardOutline,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (selected) MintAccent else ChipText,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
    )
}
