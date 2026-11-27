package dev.andre.nutritionist.android.ui.screens.role

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.domain.model.UserRole
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Stateful Route экрана выбора роли после регистрации.
 * Сохраняет выбранную роль в [AuthRepository] и направляет пользователя
 * в мастер ввода параметров ([onNavigateToOnboarding]), чтобы и клиент, и тренер
 * получили профиль и рассчитанную цель по КБЖУ на бэкенде.
 */
@Composable
fun RoleSelectionRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    RoleSelectionScreen(
        onRoleSelected = { role ->
            if (!isSubmitting) {
                isSubmitting = true
                scope.launch {
                    runCatching { authRepository.setUserRole(role) }
                    when (role) {
                        UserRole.CLIENT -> onNavigateToOnboarding()
                        UserRole.COACH -> onNavigateToOnboarding()
                    }
                }
            }
        },
        modifier = modifier,
    )
}
