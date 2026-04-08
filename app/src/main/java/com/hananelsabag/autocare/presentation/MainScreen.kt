package com.hananelsabag.autocare.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.presentation.navigation.Screen
import com.hananelsabag.autocare.presentation.screens.carprofile.CarProfileScreen
import com.hananelsabag.autocare.presentation.screens.cars.CarsScreen
import com.hananelsabag.autocare.presentation.screens.documents.CarDocumentsScreen
import com.hananelsabag.autocare.presentation.screens.documents.VehicleRecordHistoryScreen
import com.hananelsabag.autocare.presentation.screens.maintenance.MaintenanceHistoryScreen
import com.hananelsabag.autocare.presentation.screens.reminders.CarRemindersScreen
import com.hananelsabag.autocare.presentation.screens.reminders.RemindersScreen
import com.hananelsabag.autocare.presentation.screens.settings.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescriptionRes: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Cars,
        labelRes = R.string.nav_cars,
        selectedIcon = Icons.Filled.DirectionsCar,
        unselectedIcon = Icons.Outlined.DirectionsCar,
        contentDescriptionRes = R.string.nav_cars_description
    ),
    BottomNavItem(
        screen = Screen.Reminders,
        labelRes = R.string.nav_reminders,
        selectedIcon = Icons.Filled.Alarm,
        unselectedIcon = Icons.Outlined.Alarm,
        contentDescriptionRes = R.string.nav_reminders_description
    ),
    BottomNavItem(
        screen = Screen.Settings,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        contentDescriptionRes = R.string.nav_settings_description
    )
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Cars.route, Screen.Reminders.route, Screen.Settings.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                AutoCareBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Cars.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Cars.route) {
                CarsScreen(
                    onCarClick = { carId ->
                        navController.navigate(Screen.CarProfile.createRoute(carId))
                    },
                    onAddServiceForCar = { carId ->
                        navController.navigate(Screen.MaintenanceHistory.createRoute(carId))
                    }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    onCarClick = { carId ->
                        navController.navigate(Screen.CarReminders.createRoute(carId))
                    }
                )
            }

            composable(Screen.Settings.route) { SettingsScreen() }

            composable(
                route = Screen.CarProfile.route,
                arguments = listOf(
                    navArgument(Screen.CarProfile.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.CarProfile.ARG_CAR_ID)
                    ?: return@composable
                CarProfileScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() },
                    onMaintenanceHistory = {
                        navController.navigate(Screen.MaintenanceHistory.createRoute(carId))
                    },
                    onDocuments = {
                        navController.navigate(Screen.CarDocuments.createRoute(carId))
                    },
                    onReminders = {
                        navController.navigate(Screen.CarReminders.createRoute(carId))
                    }
                )
            }

            composable(
                route = Screen.MaintenanceHistory.route,
                arguments = listOf(
                    navArgument(Screen.MaintenanceHistory.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.MaintenanceHistory.ARG_CAR_ID)
                    ?: return@composable
                MaintenanceHistoryScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.VehicleRecordHistory.route,
                arguments = listOf(
                    navArgument(Screen.VehicleRecordHistory.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.VehicleRecordHistory.ARG_CAR_ID)
                    ?: return@composable
                VehicleRecordHistoryScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CarReminders.route,
                arguments = listOf(
                    navArgument(Screen.CarReminders.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.CarReminders.ARG_CAR_ID)
                    ?: return@composable
                CarRemindersScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CarDocuments.route,
                arguments = listOf(
                    navArgument(Screen.CarDocuments.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.CarDocuments.ARG_CAR_ID)
                    ?: return@composable
                CarDocumentsScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() },
                    onHistoryClick = {
                        navController.navigate(Screen.VehicleRecordHistory.createRoute(carId))
                    }
                )
            }
        }
    }
}

// ── Custom bottom nav bar ─────────────────────────────────────────────────────

@Composable
private fun AutoCareBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    // Floating pill that sits above the gesture nav area
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                NavPillItem(
                    item = item,
                    selected = currentRoute == item.screen.route,
                    onClick = { onNavigate(item.screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Icon bounces in with spring when selected
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 220),
        label = "iconColor"
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "pillColor"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(pillColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 18.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = stringResource(item.contentDescriptionRes),
                    tint = iconColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
                // Label slides in horizontally only when selected
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(180)) + expandHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Start
                    ),
                    exit = fadeOut(tween(140)) + shrinkHorizontally(
                        animationSpec = tween(220),
                        shrinkTowards = Alignment.Start
                    )
                ) {
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 7.dp)
                    )
                }
            }
        }
    }
}
