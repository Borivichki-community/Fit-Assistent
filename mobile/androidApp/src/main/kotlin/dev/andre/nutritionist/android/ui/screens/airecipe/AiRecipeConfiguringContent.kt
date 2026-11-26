package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent.Ui
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState.Content

private val mint = Color(0xFF00F5A0)
private val muted = Color(0xFF9EADC1)
private val card = Color(0xFF161922)
private val control = Color(0xFF12151C)
private val outline = Color(0xFF282D39)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiRecipeConfiguringContent(
    state: Content.Configuring,
    onEvent: (Ui) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProvideTextStyle(TextStyle(lineHeight = 14.sp, letterSpacing = 0.sp)) {
        RecipeSettingsPanel(state, onEvent, modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeSettingsPanel(state: Content.Configuring, onEvent: (Ui) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    Column(
        modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF141720))
            .border(1.dp, Brush.verticalGradient(listOf(mint.copy(alpha = 0.40f), Color.Transparent), endY = 60f), shape)
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(
            0.24f to Color.Transparent, 0.5f to mint, 0.76f to Color.Transparent,
        )))
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                .size(38.dp, 4.dp).background(Color(0xFF3A4556), CircleShape),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(32.dp).background(mint.copy(alpha = .12f), RoundedCornerShape(12.dp))
                    .border(1.dp, mint.copy(alpha = .32f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FitIcons.Sparkle, null, Modifier.size(16.dp), tint = mint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Генерация с ИИ", color = Color.White, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "PRO", Modifier.background(mint.copy(alpha = .13f), RoundedCornerShape(4.dp))
                            .border(1.dp, mint.copy(alpha = .4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        color = mint, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Text("Настройте параметры идеального приёма пищи", color = muted, fontSize = 11.sp)
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF202531))
                    .clickable(role = Role.Button) { onEvent(Ui.CancelClicked) }
                    .semantics { contentDescription = "Закрыть" },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = muted, fontSize = 20.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(outline))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecipeSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingTitle("▤", "Максимальный бюджет", mint, Modifier.weight(1f))
                    Text(
                        state.budgetLabel, Modifier.background(control, RoundedCornerShape(12.dp))
                            .border(1.dp, outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                        color = mint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.budgets.forEach { (value, label) ->
                        RecipeChip(label, state.prompt.targetBudgetRub == value) { onEvent(Ui.BudgetChanged(value)) }
                    }
                }
            }
            RecipeSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingTitle("ϟ", "Баланс КБЖУ", Color(0xFF22D3EE), Modifier.weight(1f))
                    Text("Вписать в остаток", color = mint, fontSize = 10.sp)
                    Spacer(Modifier.width(7.dp))
                    val fitRemaining = state.prompt.fitRemaining
                    Box(
                        Modifier.size(36.dp, 20.dp).clip(CircleShape)
                            .background(if (fitRemaining) mint else outline)
                            .toggleable(fitRemaining, role = Role.Switch) { onEvent(Ui.FitRemainingChanged(it)) }
                            .semantics { contentDescription = "Вписать в остаток" }
                            .padding(2.dp),
                        contentAlignment = if (fitRemaining) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(Modifier.size(16.dp).background(control, CircleShape))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().background(control, RoundedCornerShape(12.dp))
                        .border(1.dp, mint.copy(alpha = .23f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 7.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.nutrition.forEachIndexed { index, (label, value) ->
                        if (index > 0) Box(Modifier.height(20.dp).width(1.dp).background(outline))
                        Column(
                            Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(label, color = muted, fontSize = 10.sp)
                            Text(value, color = if (index == 0) mint else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            RecipeSettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingTitle("✎", "Пожелания и ингредиенты", Color(0xFFFFB020), Modifier.weight(1f))
                    Text("ИИ учтёт все детали", color = muted, fontSize = 10.sp)
                }
                BasicTextField(
                    value = state.prompt.query,
                    onValueChange = { onEvent(Ui.QueryChanged(it)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.sp),
                    cursorBrush = SolidColor(mint),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Пожелания и ингредиенты" },
                    decorationBox = { field ->
                        Box(Modifier.background(control, RoundedCornerShape(12.dp)).border(1.dp, outline, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            if (state.prompt.query.isEmpty()) {
                                Text("Сытный утренний приём без лактозы, яйца, тост или авокадо…", color = muted, fontSize = 12.sp)
                            }
                            field()
                        }
                    },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val preferences = listOf("⚡ До 15 мин", "🌾 Без глютена", "💪 Высокий белок", "🥬 Из простых продуктов")
                    preferences.forEach { label ->
                        RecipeChip(label, state.prompt.dietaryPreference == label) { onEvent(Ui.DietaryPreferenceSelected(label)) }
                    }
                }
            }
            val buttonShape = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(buttonShape)
                    .background(Brush.horizontalGradient(listOf(mint, Color(0xFF00CC79))))
                    .clickable(role = Role.Button) { onEvent(Ui.GenerateClicked) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(FitIcons.Sparkle, null, Modifier.size(16.dp), tint = control)
                Spacer(Modifier.width(7.dp))
                Text("Сгенерировать приём пищи ✨", color = control, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "Отмена", Modifier.align(Alignment.CenterHorizontally)
                    .clickable(role = Role.Button) { onEvent(Ui.CancelClicked) }.padding(horizontal = 14.dp, vertical = 2.dp),
                color = muted, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun RecipeSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(card).border(1.dp, outline, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun SettingTitle(icon: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(23.dp).background(color.copy(alpha = .10f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
            Text(icon, color = color, fontSize = 14.sp)
        }
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecipeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Text(
        label,
        Modifier.clip(shape).background(if (selected) mint.copy(alpha = .13f) else Color(0xFF1F2430))
            .border(1.dp, if (selected) mint.copy(alpha = .5f) else outline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 9.dp, vertical = 5.dp),
        color = if (selected) mint else Color(0xFFD1D9E6), fontSize = 10.sp,
    )
}

