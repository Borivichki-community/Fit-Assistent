package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent.Ui
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState.Content

private val ResultBackdrop = Color(0xA6000000)
private val ResultSheetBg = Color(0xFF12151E)
private val ResultCardBg = Color(0xFF161923)
private val ResultMint = Color(0xFF00F5A0)
private val ResultMintEnd = Color(0xFF00D680)
private val ResultOutline = Color(0x14FFFFFF)

@Composable
fun AiRecipeScreen(state: AiRecipeUiState, onEvent: (Ui) -> Unit) {
    val scrim = when (state.content) {
        is Content.Configuring -> Color(0xBF000000)
        is Content.Generating, is Content.Success, is Content.Error -> ResultBackdrop
    }
    Box(Modifier.fillMaxSize().background(scrim)) {
        when (val content = state.content) {
            is Content.Configuring -> AiRecipeConfiguringContent(
                state = content,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            is Content.Generating -> AiRecipeGeneratingContent(
                state = content,
                onCancel = { onEvent(Ui.CancelClicked) },
            )
            is Content.Success, is Content.Error -> AiRecipeResult(state, onEvent)
        }
    }
}

@Composable
private fun AiRecipeResult(state: AiRecipeUiState, onEvent: (Ui) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ResultSheetBg,
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B1F2B))
                        .border(1.dp, ResultOutline, CircleShape)
                        .clickable(role = Role.Button) { onEvent(Ui.BackClicked) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FitIcons.ChevronLeft,
                        contentDescription = "Назад",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFCBD5E1),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Генерация с ИИ",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = state.slot,
                        color = ResultMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Назад",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { onEvent(Ui.BackClicked) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                )
            }

            when (val c = state.content) {
                is Content.Configuring, is Content.Generating -> Unit
                is Content.Success -> {
                    val cardShape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(ResultCardBg)
                            .border(1.dp, ResultOutline, cardShape)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = c.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = c.badges,
                            color = ResultMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (c.description.isNotBlank()) {
                            Text(
                                text = c.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }
                        c.macros.forEach { (label, fraction) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = label, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                                LinearProgressIndicator(
                                    progress = { fraction.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = ResultMint,
                                    trackColor = Color(0xFF1F2430),
                                    strokeCap = StrokeCap.Round,
                                )
                            }
                        }
                        if (c.ingredients.isNotEmpty()) {
                            Text(
                                text = "Ингредиенты",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            c.ingredients.forEach { ingredientLine ->
                                Text(
                                    text = "• $ingredientLine",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                        if (c.instructions.isNotBlank()) {
                            Text(
                                text = "Приготовление",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = c.instructions,
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }

                    val ctaShape = RoundedCornerShape(16.dp)
                    Button(
                        onClick = { onEvent(Ui.AddRecipeToDiaryClicked) },
                        enabled = c.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(14.dp, ctaShape, ambientColor = ResultMint.copy(alpha = 0.3f), spotColor = ResultMint.copy(alpha = 0.3f))
                            .background(Brush.horizontalGradient(listOf(ResultMint, ResultMintEnd)), ctaShape),
                        shape = ctaShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF020617),
                        ),
                    ) {
                        Icon(FitIcons.Sparkle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = c.addLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = { onEvent(Ui.RegenerateClicked) },
                        enabled = c.enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Перегенерировать", color = Color(0xFFE2E8F0))
                    }
                    TextButton(
                        onClick = { onEvent(Ui.CancelClicked) },
                        enabled = c.enabled,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Отмена", color = Color(0xFF94A3B8))
                    }
                }
                is Content.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(ResultCardBg)
                            .border(1.dp, ResultOutline, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = c.message,
                            color = Color(0xFFFCA5A5),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Button(
                            onClick = { onEvent(Ui.GenerateClicked) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ResultMint,
                                contentColor = Color(0xFF020617),
                            ),
                        ) {
                            Text("Повторить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
