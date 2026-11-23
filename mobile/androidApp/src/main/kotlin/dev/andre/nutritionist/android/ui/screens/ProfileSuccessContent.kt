package dev.andre.nutritionist.android.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.ui.components.CompleteProfileBanner
import dev.andre.nutritionist.android.ui.components.FitIcons
import dev.andre.nutritionist.android.ui.components.Spacing
import dev.andre.nutritionist.android.ui.dialogs.EditActivityDialog
import dev.andre.nutritionist.android.ui.dialogs.EditBudgetDialog
import dev.andre.nutritionist.android.ui.dialogs.EditGoalDialog
import dev.andre.nutritionist.android.ui.dialogs.EditWeightDialog
import dev.andre.nutritionist.android.ui.theme.FitAssistantTheme
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel1
import dev.andre.nutritionist.android.ui.theme.GlassBorderLevel2
import dev.andre.nutritionist.android.ui.theme.TextGhost
import dev.andre.nutritionist.android.ui.theme.TextMidEmphasis
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.presentation.profile.ProfileEvent
import dev.andre.nutritionist.presentation.profile.ProfileUiModel

/**
 * Перечисление редактируемых полей метрик профиля.
 */
enum class ProfileEditField {
    WEIGHT,
    ACTIVITY,
    GOAL,
    BUDGET,
}

val CardDarkSurface = Color(0xFF141922)
val InnerDarkSurface = Color(0xFF1B202A)
val EmeraldAccent = Color(0xFF00E599)
val ProBadgeBg = Color(0xFF0B291B)
val StreakBadgeBg = Color(0xFF0E281D)
val LogoutBg = Color(0xFF201214)
val LogoutBorder = Color(0xFF5A1A1A)
val LogoutText = Color(0xFFFF5252)

/**
 * Верхняя плашка профиля в темной теме Aether Luxury:
 * • FITAPP ПРОФИЛЬ + заголовок «Профиль» + кнопка уведомлений со светодиодом.
 */
@Composable
fun ProfileTopHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FITAPP ПРОФИЛЬ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = EmeraldAccent,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Профиль",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF191F2B))
                .border(BorderStroke(1.dp, GlassBorderLevel2), CircleShape)
                .clickable { /* Уведомления */ },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = FitIcons.Notifications,
                contentDescription = "Уведомления",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(EmeraldAccent),
            )
        }
    }
}

/**
 * Контент экрана профиля в темной теме Aether Luxury,
 * полностью соответствующий дизайну screenshots/темная тема/экран профиля.png.
 */
@Composable
fun ProfileSuccessContent(
    profile: ProfileUiModel,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    isLoggingOut: Boolean = false,
    onFillSurveyClick: () -> Unit = {},
    onMealPlanClick: () -> Unit = {},
    onEvent: (ProfileEvent.Ui) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var activeDialog by remember { mutableStateOf<ProfileEditField?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!profile.isComplete) {
            CompleteProfileBanner(
                onCompleteClick = onFillSurveyClick,
                buttonText = stringResource(R.string.profile_fill_survey_again),
            )
        }

        // 1. Hero-карточка пользователя с светящимся аватаром, PRO-бейджем и страйком
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardDarkSurface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = BorderStroke(1.dp, GlassBorderLevel2),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Аватар с изумрудным неоновым кольцом и бейджем редактирования
                    Box(modifier = Modifier.size(64.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1C222D))
                                .border(BorderStroke(2.dp, EmeraldAccent), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Profile,
                                contentDescription = stringResource(R.string.content_avatar),
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        // Круглый бейдж редактирования с карандашом
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                                .clickable { onFillSurveyClick() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Edit,
                                contentDescription = "Редактировать фото",
                                tint = Color.Black,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = profile.displayName.ifBlank { "John Doe" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                            if (profile.isPro) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ProBadgeBg,
                                    border = BorderStroke(1.dp, EmeraldAccent),
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMidEmphasis,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.clickable { onFillSurveyClick() },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Редактировать данные",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = EmeraldAccent,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = FitIcons.ChevronRight,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Внутренняя плашка страйка: 14 дней подряд / Дисциплина в норме / Уровень 4
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = InnerDarkSurface,
                    border = BorderStroke(1.dp, GlassBorderLevel1),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C1E14)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Flame,
                                contentDescription = null,
                                tint = Color(0xFFFF8A00),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.streakDaysText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = profile.streakSubtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMidEmphasis,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StreakBadgeBg,
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = profile.streakLevelText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // 2. Секция: ПАРАМЕТРЫ ЗДОРОВЬЯ + Синхронизировано с Health
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ПАРАМЕТРЫ ЗДОРОВЬЯ",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = TextMidEmphasis,
            )
            Text(
                text = "Синхронизировано с Health",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldAccent,
            )
        }

        // Сетка 2x2 параметров: Вес, Активность, Цель, Бюджет
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Карточка Вес
            Card(
                onClick = { activeDialog = ProfileEditField.WEIGHT },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
                border = BorderStroke(1.dp, GlassBorderLevel1),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Вес",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMidEmphasis,
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF162638)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Plus,
                                contentDescription = "Изменить вес",
                                tint = Color(0xFF5AB2FF),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = profile.weightLabel.ifBlank { "70 кг" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StreakBadgeBg,
                    ) {
                        Text(
                            text = profile.weightChangeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Карточка Активность
            Card(
                onClick = { activeDialog = ProfileEditField.ACTIVITY },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
                border = BorderStroke(1.dp, GlassBorderLevel1),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Активность",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMidEmphasis,
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C2216)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Lightning,
                                contentDescription = null,
                                tint = Color(0xFFFF9F1C),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = profile.activityLabel.ifBlank { "Лёгкая" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profile.activitySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMidEmphasis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Карточка Цель
            Card(
                onClick = { activeDialog = ProfileEditField.GOAL },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
                border = BorderStroke(1.dp, GlassBorderLevel1),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Цель",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMidEmphasis,
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F2B20)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Target,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = profile.goalLabel.ifBlank { "Похудение" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profile.targetWeightSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldAccent,
                    )
                }
            }

            // Карточка Бюджет
            Card(
                onClick = { activeDialog = ProfileEditField.BUDGET },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
                border = BorderStroke(1.dp, GlassBorderLevel1),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Бюджет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMidEmphasis,
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF11262B)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = FitIcons.Wallet,
                                contentDescription = null,
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = profile.dailyBudgetText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val budgetSubText = when {
                        profile.weeklyBudgetLabel.isNotBlank() && !profile.weeklyBudgetLabel.startsWith("~") ->
                            profile.weeklyBudgetLabel
                        else -> profile.weeklyBudgetText
                    }

                    Text(
                        text = budgetSubText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMidEmphasis,
                    )
                }
            }
        }

        // 3. Секция: СУТОЧНАЯ НОРМА КБЖУ (2 400 ккал + 3 колонки: Белки, Жиры, Углеводы)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "СУТОЧНАЯ НОРМА КБЖУ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    Text(
                        text = profile.caloriesGoalLabel.ifBlank { "2 400 ккал" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = EmeraldAccent,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Белки
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = InnerDarkSurface,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Белки",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMidEmphasis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.proteinsGoalLabel.ifBlank { "170 г" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }
                    }

                    // Жиры
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = InnerDarkSurface,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Жиры",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMidEmphasis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.fatsGoalLabel.ifBlank { "85 г" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }
                    }

                    // Углеводы
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = InnerDarkSurface,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Углеводы",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMidEmphasis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.carbsGoalLabel.ifBlank { "378 г" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // 4. Карточка меню настроек: Настройки, План питания и цели, Уведомления, Помощь
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarkSurface),
            border = BorderStroke(1.dp, GlassBorderLevel1),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileMenuRow(
                    icon = FitIcons.Settings,
                    iconContainerColor = Color(0xFF192233),
                    iconTint = Color(0xFF5AB2FF),
                    title = "Настройки",
                    subtitle = "Аккаунт, интеграции, безопасность",
                    onClick = onEditClick,
                )

                HorizontalDivider(color = GlassBorderLevel1)

                ProfileMenuRow(
                    icon = FitIcons.Meals,
                    iconContainerColor = Color(0xFF261D33),
                    iconTint = Color(0xFFB072FF),
                    title = "План питания и цели",
                    subtitle = profile.planSummaryText,
                    onClick = onMealPlanClick,
                )

                HorizontalDivider(color = GlassBorderLevel1)

                ProfileMenuRow(
                    icon = FitIcons.Notifications,
                    iconContainerColor = Color(0xFF152A2D),
                    iconTint = Color(0xFF22D3EE),
                    title = "Уведомления",
                    subtitle = "Напоминания о приёмах пищи и воде",
                    trailingBadge = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StreakBadgeBg,
                        ) {
                            Text(
                                text = "Вкл",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    },
                    onClick = { /* Настройки уведомлений */ },
                )

                HorizontalDivider(color = GlassBorderLevel1)

                ProfileMenuRow(
                    icon = FitIcons.Help,
                    iconContainerColor = Color(0xFF22242D),
                    iconTint = Color(0xFFCBD5E1),
                    title = "Помощь",
                    subtitle = "Частые вопросы, поддержка куратора",
                    onClick = { /* Справка */ },
                )
            }
        }

        // 5. Кнопка выхода из аккаунта (красная рамка и фон)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoggingOut) { onLogoutClick() },
            shape = RoundedCornerShape(16.dp),
            color = LogoutBg,
            border = BorderStroke(1.dp, LogoutBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FitIcons.Logout,
                    contentDescription = null,
                    tint = LogoutText,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.profile_logout_button),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = LogoutText,
                )
            }
        }

        // 6. Версия приложения
        Text(
            text = profile.versionText,
            style = MaterialTheme.typography.bodySmall,
            color = TextGhost,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
        )
    }

    // Модальные диалоги точечного редактирования
    when (activeDialog) {
        ProfileEditField.WEIGHT -> {
            EditWeightDialog(
                initialWeight = profile.rawWeightKg,
                onDismiss = { activeDialog = null },
                onConfirm = { newWeight ->
                    onEvent(ProfileEvent.Ui.ChangeWeight(newWeight))
                    activeDialog = null
                },
            )
        }
        ProfileEditField.ACTIVITY -> {
            EditActivityDialog(
                initialActivity = profile.rawActivityLevel,
                onDismiss = { activeDialog = null },
                onConfirm = { newActivity ->
                    onEvent(ProfileEvent.Ui.ChangeActivity(newActivity))
                    activeDialog = null
                },
            )
        }
        ProfileEditField.GOAL -> {
            EditGoalDialog(
                initialGoal = profile.rawGoalType,
                onDismiss = { activeDialog = null },
                onConfirm = { newGoal ->
                    onEvent(ProfileEvent.Ui.ChangeGoal(newGoal))
                    activeDialog = null
                },
            )
        }
        ProfileEditField.BUDGET -> {
            EditBudgetDialog(
                initialBudget = profile.rawWeeklyBudget,
                onDismiss = { activeDialog = null },
                onConfirm = { newBudget ->
                    onEvent(ProfileEvent.Ui.ChangeBudget(newBudget))
                    activeDialog = null
                },
            )
        }
        null -> Unit
    }
}

/**
 * Строка меню настроек профиля с цветной иконкой, заголовком, подзаголовком и шевроном.
 */
@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingBadge: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMidEmphasis,
            )
        }

        if (trailingBadge != null) {
            trailingBadge()
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = FitIcons.ChevronRight,
            contentDescription = null,
            tint = TextGhost,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(name = "ProfileSuccessContent - Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileSuccessContentDarkPreview() {
    FitAssistantTheme(darkTheme = true) {
        ProfileSuccessContent(
            profile = ProfileUiModel(
                id = ProfileId("p1"),
                userId = UserId("u1"),
                displayName = "John Doe",
                email = "john.athlete@fitapp.io",
                isPro = true,
                heightLabel = "180 см",
                weightLabel = "70 кг",
                genderLabel = "Мужской",
                activityLabel = "Лёгкая",
                goalLabel = "Похудение",
                caloriesGoalLabel = "2 400 ккал",
                proteinsGoalLabel = "170 г",
                fatsGoalLabel = "85 г",
                carbsGoalLabel = "378 г",
                weightGoalLabel = "65 кг",
                weeklyBudgetLabel = "~3 500 ₽ в неделю",
                dailyBudgetText = "500 ₽ / день",
                weeklyBudgetText = "~3 500 ₽ в неделю",
            ),
            onEditClick = {},
            onLogoutClick = {},
        )
    }
}
