package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.presentation.airecipe.*
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent.Ui
import dev.andre.nutritionist.presentation.airecipe.AiRecipeUiState.Content

private val emerald = Color(0xFF00E599)
private val gradient = Brush.horizontalGradient(listOf(emerald, Color(0xFF00B377)))

@Composable
fun AiRecipeScreen(state: AiRecipeUiState, onEvent: (Ui) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xB0000000))) {
        when (val content = state.content) {
            is Content.Configuring -> AiRecipeConfiguringContent(
                content,
                onEvent,
                Modifier.align(Alignment.BottomCenter),
            )
            is Content.Generating -> AiRecipeGeneratingContent(content) { onEvent(Ui.CancelClicked) }
            is Content.Success, is Content.Error -> AiRecipeResult(state, onEvent)
        }
    }
}

@Composable
private fun AiRecipeResult(state: AiRecipeUiState, onEvent: (Ui) -> Unit) {
    Surface(color = Color(0xFF141720), contentColor = Color.White) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton({ onEvent(Ui.BackClicked) }) { Text("Назад") }
                Text("Генерация с ИИ", style = MaterialTheme.typography.titleLarge)
            }
            Text(state.slot, color = emerald)
            when (val c = state.content) {
                is Content.Configuring, is Content.Generating -> Unit
                is Content.Success -> {
                    Card(Modifier.weight(1f).fillMaxWidth(),colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(c.title,style = MaterialTheme.typography.headlineSmall)
                            Text(c.badges)
                            Text(c.description)
                            c.macros.forEach { (label,fraction) ->
                                Text(label)
                                LinearProgressIndicator(progress = { fraction },color = emerald)
                            }
                            Text("Ингредиенты")
                            c.ingredients.forEach { Text(it) }
                            Text(c.instructions)
                        }
                    }
                    Primary(c.addLabel,c.enabled) { onEvent(Ui.AddRecipeToDiaryClicked) }
                    OutlinedButton({ onEvent(Ui.RegenerateClicked) },Modifier.fillMaxWidth(),enabled = c.enabled) { Text("Перегенерировать") }
                    TextButton({ onEvent(Ui.CancelClicked) },enabled = c.enabled) { Text("Отмена") }
                }
                is Content.Error -> {
                    Text(c.message)
                    Button({ onEvent(Ui.GenerateClicked) }) { Text("Повторить") }
                }
            }
        }
    }
}

@Composable
private fun Primary(label: String, enabled: Boolean = true, sparkle: Boolean = false, onClick: () -> Unit) {
    Button(onClick,Modifier.fillMaxWidth().background(gradient,RoundedCornerShape(28.dp)),enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,contentColor = Color.Black)) {
        if (sparkle) {
            Icon(dev.andre.nutritionist.android.ui.components.FitIcons.Sparkle,null)
            Spacer(Modifier.width(8.dp))
        }
        Text(label)
    }
}
