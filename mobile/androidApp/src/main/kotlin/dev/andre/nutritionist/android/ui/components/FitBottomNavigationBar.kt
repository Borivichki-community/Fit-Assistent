package dev.andre.nutritionist.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.android.navigation.AppDestination

/**
 * Нижняя плавающая панель навигации приложения
 */
@Composable
fun FitBottomNavigationBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        shape = RoundedCornerShape(Spacing.buttonRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.small, horizontal = Spacing.small),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomNavItem(
                icon = FitIcons.Document,
                contentDescription = stringResource(R.string.nav_diary),
                isSelected = currentDestination == AppDestination.Dashboard || currentDestination == AppDestination.Diary,
                onClick = { onDestinationSelected(AppDestination.Dashboard) },
            )
            BottomNavItem(
                icon = FitIcons.Meals,
                contentDescription = stringResource(R.string.nav_recipes),
                isSelected = currentDestination == AppDestination.Recipes,
                onClick = { onDestinationSelected(AppDestination.Recipes) },
            )
            BottomNavItem(
                icon = FitIcons.Statistics,
                contentDescription = stringResource(R.string.nav_statistics),
                isSelected = currentDestination == AppDestination.Statistics,
                onClick = { onDestinationSelected(AppDestination.Statistics) },
            )
            BottomNavItem(
                icon = FitIcons.Profile,
                contentDescription = stringResource(R.string.nav_profile),
                isSelected = currentDestination == AppDestination.Profile,
                onClick = { onDestinationSelected(AppDestination.Profile) },
            )
        }
    }
}
